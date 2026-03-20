<template>
  <el-dialog title="登记催收记录" :visible="dialogVisible" width="620px" @close="handleClose">
    <el-form :model="form" label-width="110px">
      <el-form-item label="催收时间">
        <el-date-picker
          v-model="form.actualCollectionTime"
          type="datetime"
          value-format="yyyy-MM-dd HH:mm:ss"
          placeholder="请选择催收时间"
          style="width: 100%;"
        ></el-date-picker>
      </el-form-item>
      <el-form-item label="催收对象">
        <el-select v-model="form.targetType" style="width: 100%;" @change="onTargetTypeChange">
          <el-option label="借款人" value="borrower"></el-option>
          <el-option label="担保人" value="guarantor"></el-option>
          <el-option label="其他" value="other"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="催收对象姓名">
        <el-input v-model="form.targetName" :disabled="form.targetType === 'borrower'" placeholder="选择借款人自动填入，否则请手动填写"></el-input>
      </el-form-item>
      <el-form-item label="催收方式">
        <el-select v-model="form.method" style="width: 100%;" @change="onMethodChange">
          <el-option
            v-for="item in collectionMethodOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="催收结果">
        <el-input v-model="form.result" placeholder="请输入本次催收结果"></el-input>
      </el-form-item>
      <el-form-item label="备注">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入备注信息"
        ></el-input>
      </el-form-item>
      <el-form-item label="附件材料">
        <el-upload
          action="#"
          :auto-upload="false"
          :show-file-list="true"
          :on-change="onMaterialFileChange"
          :file-list="uploadFileList"
          :limit="1"
        >
          <el-button size="small" type="primary">
            选择{{ currentMaterialLabel }}
          </el-button>
        </el-upload>
        <div style="color: #909399; font-size: 12px; margin-top: 5px; line-height: 1.8;">
          <div>{{ fileTypeTip }}</div>
          <div>• 仅支持单个文件上传</div>
          <div>• 支持压缩文件(zip/rar/7z)</div>
          <div>• 文件不超过 10MB</div>
        </div>
      </el-form-item>
    </el-form>
    <span slot="footer">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确认登记</el-button>
    </span>
  </el-dialog>
</template>

<script>
export default {
  name: 'CollectionRecordDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    customerName: {
      type: String,
      default: ''
    },
    collectionMethodOptions: {
      type: Array,
      default: () => []
    }
  },
  data () {
    return {
      form: {
        actualCollectionTime: '',
        targetType: 'borrower',
        targetName: '',
        method: 'phone',
        result: '',
        remark: ''
      },
      uploadFileList: [],
      rawFile: null
    }
  },
  computed: {
    dialogVisible: {
      get () {
        return this.visible
      },
      set (val) {
        this.$emit('update:visible', val)
      }
    },
    currentMaterialLabel () {
      const option = this.collectionMethodOptions.find(item => item.value === this.form.method)
      return option ? option.materialLabel : '文件'
    },
    fileTypeTip () {
      const option = this.collectionMethodOptions.find(item => item.value === this.form.method)
      if (!option) {
        return '• 支持常见文件格式'
      }
      const typeLabels = {
        audio: '• 支持 mp3、wav、aac 等音频格式',
        image: '• 支持 jpg、png、pdf 等图片或文档格式',
        document: '• 支持 pdf、doc、docx、zip、rar、7z 等文档或压缩文件'
      }
      return typeLabels[option.materialType] || '• 支持常见文件格式'
    },
    currentMaterialTip () {
      return ''
    }
  },
  methods: {
    onTargetTypeChange () {
      if (this.form.targetType === 'borrower') {
        this.form.targetName = this.customerName || ''
      } else {
        this.form.targetName = ''
      }
    },
    onMethodChange () {
      this.uploadFileList = []
      this.rawFile = null
    },
    onMaterialFileChange (file, fileList) {
      this.uploadFileList = fileList.slice(-1)
      this.rawFile = file.raw || null
    },
    handleClose () {
      this.$emit('update:visible', false)
      this.resetForm()
    },
    handleCancel () {
      this.$emit('update:visible', false)
      this.resetForm()
    },
    handleConfirm () {
      this.$emit('submit', {
        form: this.form,
        rawFile: this.rawFile
      })
    },
    resetForm () {
      this.form = {
        actualCollectionTime: '',
        targetType: 'borrower',
        targetName: '',
        method: 'phone',
        result: '',
        remark: ''
      }
      this.uploadFileList = []
      this.rawFile = null
    }
  }
}
</script>
