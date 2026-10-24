<template>
  <section>
    <div class="page-head">
      <div>
        <h1>分类</h1>
        <p class="lede">书的数量由字段解析器统计。分类下还有书时不能删除。</p>
      </div>
      <button class="primary" type="button" @click="openCreate">新分类</button>
    </div>
    <p v-if="error" class="banner">{{ error }}</p>
    <table>
      <thead>
        <tr><th>名称</th><th>说明</th><th>书</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="item in categories" :key="item.id">
          <td>{{ item.name }}</td>
          <td>{{ item.description || '—' }}</td>
          <td>{{ item.bookCount }}</td>
          <td class="row-actions">
            <button class="quiet" type="button" @click="openEdit(item)">修改</button>
            <button v-if="confirmId !== item.id" class="danger" type="button" @click="confirmId = item.id">删除</button>
            <button v-else class="danger" type="button" @click="remove(item)">确认删除</button>
          </td>
        </tr>
      </tbody>
    </table>
    <Modal :open="editing" :title="form.id ? '修改分类' : '新分类'" @close="editing = false">
      <form @submit.prevent="save">
        <label class="field"><span>名称</span><input v-model="form.name" required /></label>
        <label class="field"><span>说明</span><textarea v-model="form.description"></textarea></label>
        <button class="primary" type="submit">保存</button>
      </form>
    </Modal>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import Modal from '../components/Modal.vue'
import { gql } from '../api/graphql'

const categories = ref([])
const error = ref('')
const editing = ref(false)
const confirmId = ref(null)
const form = reactive({ id: null, name: '', description: '' })

async function load() {
  error.value = ''
  confirmId.value = null
  try {
    const data = await gql(`query { categories { id name description bookCount } }`)
    categories.value = data.categories
  } catch (e) {
    error.value = e.message
  }
}

function openCreate() {
  Object.assign(form, { id: null, name: '', description: '' })
  editing.value = true
}

function openEdit(item) {
  Object.assign(form, { id: item.id, name: item.name, description: item.description || '' })
  editing.value = true
}

async function save() {
  try {
    if (form.id) {
      await gql(`mutation($input: CategoryUpdateInput!) { updateCategory(input: $input) { id } }`, {
        input: { id: form.id, name: form.name, description: form.description }
      })
    } else {
      await gql(`mutation($input: CategoryInput!) { createCategory(input: $input) { id } }`, {
        input: { name: form.name, description: form.description || null }
      })
    }
    editing.value = false
    await load()
  } catch (e) {
    error.value = e.message
    editing.value = false
  }
}

async function remove(item) {
  try {
    await gql(`mutation($id: Int!) { deleteCategory(id: $id) }`, { id: item.id })
    await load()
  } catch (e) {
    error.value = e.message
  }
}

onMounted(load)
</script>
