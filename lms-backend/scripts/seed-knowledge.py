#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
知识库种子初始化脚本（NL2SQL RAG 召回用）。

读取 knowledge-seed.json，逐条写入知识库 API，供测试环境直接生成 RAG 条目：
  - category=schema      库信息（字段口径 / 枚举 / 指标定义）
  - category=sql-example SQL 用法示例（问题 → 单表 SELECT）
这两类会被 NL2SQL 规划阶段向量召回；business 仅用于 chat 分支。

幂等策略（可重复执行）：
  1. GET  /knowledge/{title}  判断条目是否存在
  2. 存在 → PUT  /knowledge/{title}  更新（默认）
     -f 时先 DELETE 再 POST，强制重建切块
  3. 不存在 → POST /knowledge       新增

认证说明：
  测试环境 auth.enabled=false（或 auth.token-check-url 未配置）时，任意
  Bearer token 都会被放行，脚本用占位 token 即可。
  若测试环境开启了真实 OAuth 校验，需用 -t 传入真实 access_token。

用法：
  python3 seed-knowledge.py                          # 默认 http://127.0.0.1:8080
  python3 seed-knowledge.py -b http://192.168.1.10:8080
  python3 seed-knowledge.py -t <真实token> -d ./my-knowledge.json
  python3 seed-knowledge.py -f -n                     # 预览强制重建计划（dry-run）
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

DEFAULT_DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'knowledge-seed.json')
DEFAULT_TOKEN = 'seed-knowledge-script'  # 测试环境 auth 未开时任意值均放行
VALID_CATEGORIES = ('schema', 'sql-example', 'business')


def build_args():
    p = argparse.ArgumentParser(description='知识库种子初始化（NL2SQL RAG 召回）')
    p.add_argument('-b', '--base', default='http://127.0.0.1:8080',
                   help='后端 API 基地址（默认 http://127.0.0.1:8080，有网关前缀则含前缀）')
    p.add_argument('-t', '--token', default='', help='Bearer token（测试环境 auth 未开时可不传）')
    p.add_argument('-d', '--data', default=DEFAULT_DATA, help='知识条目 JSON 文件（默认同目录 knowledge-seed.json）')
    p.add_argument('-f', '--force', action='store_true',
                   help='强制重建：条目已存在时先 DELETE 再 POST（重新切块向量化）')
    p.add_argument('-n', '--dry-run', action='store_true', help='只打印计划，不真正调用接口')
    p.add_argument('--limit', type=int, default=0, help='只处理前 N 条（调试用，0=全部）')
    return p.parse_args()


def request(base, token, method, path, body=None):
    """发送 HTTP 请求，返回 (http_status, response_body_str)。"""
    url = base.rstrip('/') + path
    req = urllib.request.Request(url, method=method)
    req.add_header('Authorization', 'Bearer ' + token)
    req.add_header('Content-Type', 'application/json; charset=utf-8')
    data = None
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode('utf-8')
    try:
        with urllib.request.urlopen(req, data=data, timeout=30) as resp:
            return resp.status, resp.read().decode('utf-8')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8')
    except urllib.error.URLError as e:
        return 0, '连接失败: %s' % e.reason


def parse_result(body):
    """把响应体解析为 Result JSON；解析失败返回 None。"""
    try:
        return json.loads(body)
    except Exception:
        return None


def title_path(title):
    return '/knowledge/' + urllib.parse.quote(title, safe='')


def is_ok(status, body):
    """业务成功判定：HTTP 200 且返回体 code=='0'（后端失败也返回 HTTP 200 + code 400）。"""
    r = parse_result(body)
    return status == 200 and r is not None and r.get('code') == '0'


def main():
    args = build_args()

    token = args.token if args.token else DEFAULT_TOKEN
    if not args.token:
        print('[提示] 未传 -t。测试环境 auth.enabled=false 或 auth.token-check-url 为空时任意 token 均放行；')
        print('       若测试环境开启了真实 OAuth 校验，请用 -t 传入有效 access_token。')

    if not os.path.exists(args.data):
        print('[错误] 数据文件不存在: %s' % args.data)
        sys.exit(1)
    with open(args.data, 'r', encoding='utf-8') as f:
        try:
            items = json.load(f)
        except Exception as e:
            print('[错误] 数据文件 JSON 解析失败: %s' % e)
            sys.exit(1)
    if not isinstance(items, list) or len(items) == 0:
        print('[错误] 数据文件应为非空 JSON 数组')
        sys.exit(1)

    if not args.dry_run:
        # 连通性预检：列表接口 401/403 说明 token 无效，直接提示退出
        status, body = request(args.base, token, 'GET', '/knowledge/list')
        if status in (401, 403):
            print('[认证失败] /knowledge/list 返回 HTTP %s，token 无效或 auth 已开启。' % status)
            print('           请用 -t 传入真实 OAuth access_token 后重试。')
            sys.exit(2)
        if status == 0:
            print('[连接失败] 无法访问 %s，请检查后端是否已启动、地址是否正确（-b）。' % args.base)
            sys.exit(1)
        print('[预检] API 连通正常: %s/knowledge/list (HTTP %s)\n' % (args.base, status))
    else:
        print('[dry-run] 跳过预检，仅输出计划\n')

    if args.limit > 0:
        items = items[:args.limit]

    print('共 %d 条知识，开始%s...\n' % (len(items), '计划输出（dry-run）' if args.dry_run else '写入'))
    ok_count, fail_count = 0, 0

    for i, item in enumerate(items, 1):
        title = (item.get('title') or '').strip()
        category = item.get('category')
        content = (item.get('content') or '').strip()

        if not title or not content:
            print('[%02d] 跳过: title/content 为空 (%s)' % (i, title or '<无标题>'))
            fail_count += 1
            continue
        if category not in VALID_CATEGORIES:
            print('[%02d] 警告: category=%r 非预设枚举 %s，仍按原值写入' % (i, category, VALID_CATEGORIES))

        action = '新增'
        if not args.dry_run:
            s, b = request(args.base, token, 'GET', title_path(title))
            if s == 200:
                action = '强制重建' if args.force else '更新'
            elif s in (401, 403):
                print('[%02d] 认证失败(%s): 请用 -t 传有效 token' % (i, s))
                fail_count += 1
                continue
            elif s != 404:
                print('[%02d] 警告: 查询返回 HTTP %s，按不存在处理' % (i, s))

        if args.dry_run:
            print('[%02d] [dry-run] %s [%s] %s' % (i, action, category or '-', title))
            ok_count += 1
            continue

        result_body = None
        if args.force and action == '强制重建':
            s, b = request(args.base, token, 'DELETE', title_path(title))
            if not is_ok(s, b):
                print('[%02d] 失败(删除旧条目): %s %s' % (i, s, b[:120]))
                fail_count += 1
                continue
            action = '重建(删后新增)'
            body = {'title': title, 'category': category, 'content': content}
            s, b = request(args.base, token, 'POST', '/knowledge', body)
        elif action == '更新':
            body = {'category': category, 'content': content}
            s, b = request(args.base, token, 'PUT', title_path(title), body)
        else:
            body = {'title': title, 'category': category, 'content': content}
            s, b = request(args.base, token, 'POST', '/knowledge', body)

        if is_ok(s, b):
            ok_count += 1
            print('[%02d] 成功 [%s] %s (%s)' % (i, action, title, category or '-'))
        else:
            fail_count += 1
            print('[%02d] 失败 [%s] %s -> HTTP %s %s' % (i, action, title, s, b[:150]))

    print('\n===== 汇总 =====')
    print('总数: %d, 成功: %d, 失败: %d' % (len(items), ok_count, fail_count))
    if args.dry_run:
        print('（dry-run 未真正调用接口）')
    if fail_count == 0:
        print('全部成功。后端每次写入自动重建向量索引，可直接提问测试召回效果。')
        print('验证: 启动日志应见「知识库向量索引刷新完成」；提问后查 ai_query_audit_log.params.rawSql 确认 LLM 参考了示例。')
    else:
        print('存在失败条目，请结合上方错误信息处理后再执行一次（脚本幂等，可重复跑）。')
        sys.exit(1)


if __name__ == '__main__':
    main()
