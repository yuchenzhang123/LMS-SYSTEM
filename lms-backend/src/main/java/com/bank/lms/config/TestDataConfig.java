package com.bank.lms.config;

import com.bank.lms.entity.CollectionRecord;
import com.bank.lms.entity.Litigation;
import com.bank.lms.entity.LoanAccount;
import com.bank.lms.entity.Notice;
import com.bank.lms.repository.CollectionRecordRepository;
import com.bank.lms.repository.LitigationRepository;
import com.bank.lms.repository.LoanAccountRepository;
import com.bank.lms.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 测试数据初始化配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TestDataConfig {

    private final LoanAccountRepository loanAccountRepository;
    private final CollectionRecordRepository collectionRecordRepository;
    private final LitigationRepository litigationRepository;
    private final NoticeRepository noticeRepository;

    @Value("${lms.dev.generate-test-data:false}")
    private boolean generateTestData;

    @Bean
    @Profile("dev")
    public CommandLineRunner initTestData() {
        return args -> {
            if (!generateTestData) {
                log.info("跳过测试数据生成");
                return;
            }

            // 检查是否已有数据
            if (loanAccountRepository.count() > 0) {
                log.info("数据库已有数据，跳过初始化");
                return;
            }

            log.info("开始生成测试数据...");

            // 初始化贷款账户
            LoanAccount account1 = createAccount("LA202501010001", "8800231", "张三", "广州市越秀支行",
                    "13800138000", "XFD001", 45, "collecting");

            LoanAccount account2 = createAccount("LA202502020002", "8800231", "张三", "广州市越秀支行",
                    "13800138000", "XFY002", 30, "uncollected");

            LoanAccount account3 = createAccount("LA202503030003", "8800232", "李四", "广州市天河支行",
                    "13900139000", "XFD001", 15, "completed");

            loanAccountRepository.saveAll(Arrays.asList(account1, account2, account3));
            log.info("已创建 {} 个贷款账户", 3);

            // 初始化催收记录
            CollectionRecord r1 = createRecord("R1001", "LA202501010001", "8800231", "张三",
                    "sms", "短信", "已发送提醒短信", "954", "开发管理员");

            CollectionRecord r2 = createRecord("R1002", "LA202501010001", "8800231", "张三",
                    "phone", "电话", "客户承诺 3 日内处理", "1001", "业务员A");

            CollectionRecord r3 = createRecord("R1003", "LA202502020002", "8800231", "张三",
                    "visit", "上门", "已上门核实客户经营情况", "1001", "业务员A");

            collectionRecordRepository.saveAll(Arrays.asList(r1, r2, r3));
            log.info("已创建 {} 条催收记录", 3);

            // 初始化诉讼信息
            Litigation l1 = createLitigation("L20260001", "LA202501010001", "8800231", "张三",
                    "2.2", "已立案待开庭", true, "广州市越秀区人民法院", "广东正衡律师事务所");

            Litigation l2 = createLitigation("L20250001", "LA202501010001", "8800231", "张三",
                    "3.7", "终结执行【注意2年内恢复执行，一般3个月内恢复执行】", false, "广州市越秀区人民法院", "广东正衡律师事务所");

            litigationRepository.saveAll(Arrays.asList(l1, l2));
            log.info("已创建 {} 条诉讼记录", 2);

            // 初始化通知
            Notice n1 = createNotice("N1001", "客户 8800231 贷款账户逾期提醒", "high",
                    "客户 8800231 的贷款账户 LA202501010001 已逾期 45 天，建议尽快完成电话提醒。",
                    "8800231", "LA202501010001", "张三", "XFD001", 45);

            Notice n2 = createNotice("N1002", "客户 8800232 贷款账户逾期提醒", "medium",
                    "客户 8800232 的贷款账户 LA202503030003 已逾期 15 天，请及时跟进。",
                    "8800232", "LA202503030003", "李四", "XFD001", 15);

            Notice n3 = createNotice("N1003", "新的催收任务分配", "high",
                    "您有新的催收账户需要处理，请及时查看。",
                    "8800231", "LA202502020002", "张三", "XFY002", 30);

            noticeRepository.saveAll(Arrays.asList(n1, n2, n3));
            log.info("已创建 {} 条通知", 3);

            log.info("测试数据生成完成！");
        };
    }

    private LoanAccount createAccount(String loanAccount, String customerId, String customerName,
                                      String orgName, String phone, String productCode,
                                      int overdueDays, String status) {
        LoanAccount account = new LoanAccount();
        account.setLoanAccount(loanAccount);
        account.setCustomerId(customerId);
        account.setCustomerName(customerName);
        account.setOrgName(orgName);
        account.setPhone(phone);
        account.setProductCode(productCode);
        account.setLoanDate(LocalDate.now().minusMonths(3));
        account.setLoanTerm(12);
        account.setOverdueDays(overdueDays);
        account.setContractAmount(new BigDecimal("100000.00"));
        account.setLoanBalance(new BigDecimal("85000.00"));
        account.setUnexpiredPrincipal(new BigDecimal("70000.00"));
        account.setOverduePrincipal(new BigDecimal("15000.00"));
        account.setOverdueInterest(new BigDecimal("450.00"));
        account.setOverduePenalty(new BigDecimal("225.00"));
        account.setTotalOverdueAmount(new BigDecimal("15675.00"));
        account.setStatus(status);
        return account;
    }

    private CollectionRecord createRecord(String recordId, String loanAccount, String customerId,
                                          String customerName, String method, String methodText,
                                          String result, String operatorId, String operatorName) {
        CollectionRecord record = new CollectionRecord();
        record.setRecordId(recordId);
        record.setLoanAccount(loanAccount);
        record.setCustomerId(customerId);
        record.setCustomerName(customerName);
        record.setMethod(method);
        record.setMethodText(methodText);
        record.setResult(result);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setOperateTime(LocalDateTime.now().minusDays(5));
        return record;
    }

    private Litigation createLitigation(String litigationId, String loanAccount, String customerId,
                                        String customerName, String statusCode, String statusText,
                                        boolean inLitigation, String courtName, String lawFirm) {
        Litigation litigation = new Litigation();
        litigation.setLitigationId(litigationId);
        litigation.setLoanAccount(loanAccount);
        litigation.setCustomerId(customerId);
        litigation.setCustomerName(customerName);
        litigation.setStatusCode(statusCode);
        litigation.setStatusText(statusText);
        litigation.setInLitigation(inLitigation);
        litigation.setCourtName(courtName);
        litigation.setLawFirm(lawFirm);
        litigation.setRemark("测试数据");
        return litigation;
    }

    private Notice createNotice(String noticeId, String title, String level, String message,
                                String customerId, String loanAccount, String customerName,
                                String productCode, int overdueDays) {
        Notice notice = new Notice();
        notice.setNoticeId(noticeId);
        notice.setTitle(title);
        notice.setLevel(level);
        notice.setMessage(message);
        notice.setCustomerId(customerId);
        notice.setLoanAccount(loanAccount);
        notice.setCustomerName(customerName);
        notice.setProductCode(productCode);
        notice.setOverdueDays(overdueDays);
        notice.setIsRead(false);
        return notice;
    }
}
