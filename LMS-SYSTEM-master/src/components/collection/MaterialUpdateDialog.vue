<template>
  <el-dialog
    title="材料补交/重交"
    :visible="dialogVisible"
    width="500px"
    @close="handleClose"
  >
    <el-form :model="form" label-width="100px">
      <el-form-item label="附件材料">
        <el-upload
          action="#"
          :auto-upload="false"
          :show-file-list="true"
          :on-change="onFileChange"
          :file-list="fileList"
          :limit="1"
        >
          <el-button size="small" type="primary">
            选择{{ currentMaterialLabel }}
          </el-button>
        </el-upload>
        <div style="color: #909399; font-size: 12px; margin-top: 5px; line-height: 1.8;">
          <div>• 支持常见文件格式</div>
          <div>• 支持压缩文件(zip/rar/7z)</div>
          <div>• 仅支持单个文件上传</div>
          <div>• 文件不超过 10MB</div>
        </div>
      </el-form-item>
    </el-form>
    <span slot="footer">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :loading="loading">确认更新</el-button>
    </span>
  </el-dialog>
</template>

<script>
export default {
  name: 'MaterialUpdateDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    recordId: {
      type: String,
      default: ''
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      form: {
        recordId: '',
        materialType: '',
        materialName: '',
        materialUrl: ''
      },
      fileList: [],
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
      return '材料文件'
    },
    currentMaterialTip () {
      return ''
    }
  },
  watch: {
    recordId: {
      handler (val) {
        this.form.recordId = val
      },
      immediate: true
    }
  },
  methods: {
    onFileChange (file, fileList) {
      this.fileList = fileList.slice(-1)
      this.rawFile = file.raw || null
      // 更新材料名称
      if (file.raw) {
        this.form.materialName = file.raw.name
      }
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
        recordId: '',
        materialType: '',
        materialName: '',
        materialUrl: ''
      }
      this.fileList = []
      this.rawFile = null
    },
    setInitialData (row) {
      this.form = {
        recordId: row.id,
        materialType: row.materialType || '',
        materialName: row.materialName || '',
        materialUrl: row.materialUrl || ''
      }
      this.fileList = []
      this.rawFile = null
    }
  }
}
</script>
