<template>
  <section>
    <div class="page-head">
      <div>
        <h1>读者</h1>
        <p class="lede">停用的读者不能新建订阅。还有订阅记录时不能删除读者。</p>
      </div>
      <button class="primary" type="button" @click="openCreate">登记读者</button>
    </div>
    <p v-if="error" class="banner">{{ error }}</p>
    <div class="filters">
      <label class="field grow"><span>姓名或邮箱</span><input v-model="keyword" @keyup.enter="reload" /></label>
      <label class="field">
        <span>状态</span>
        <select v-model="status">
          <option value="">全部</option>
          <option value="ACTIVE">可用</option>
          <option value="DISABLED">停用</option>
        </select>
      </label>
      <button class="quiet" type="button" @click="reload">筛选</button>
    </div>
    <table>
      <thead>
        <tr><th>姓名</th><th>邮箱</th><th>电话</th><th>状态</th><th>登记时间</th><th></th></tr>
      </thead>
      <tbody>
        <template v-for="reader in readers" :key="reader.id">
          <tr>
            <td>{{ reader.username }}</td>
            <td>{{ reader.email }}</td>
            <td>{{ reader.phone || '—' }}</td>
            <td>{{ readerStatusLabel[reader.status] }}</td>
            <td>{{ when(reader.createdAt) }}</td>
            <td class="row-actions">
              <button class="linkish" type="button" @click="toggle(reader)">订阅</button>
              <button class="quiet" type="button" @click="openEdit(reader)">修改</button>
              <button v-if="confirmId !== reader.id" class="danger" type="button" @click="confirmId = reader.id">删除</button>
              <button v-else class="danger" type="button" @click="remove(reader)">确认删除</button>
            </td>
          </tr>
          <tr v-if="openId === reader.id">
            <td colspan="6" class="nested">
              <span v-if="!detail.length">没有订阅。</span>
              <span v-for="item in detail" :key="item.id">
                {{ item.book.name }} / {{ planLabel[item.plan] }} / {{ membershipStatusLabel[item.status] }}
              </span>
            </td>
          </tr>
        </template>
        <tr v-if="!readers.length"><td colspan="6" class="empty">没有匹配的读者。</td></tr>
      </tbody>
    </table>
    <div class="pager">
      <button class="quiet" type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span>第 {{ pageInfo.page || page }} / {{ pageInfo.totalPages || 1 }} 页，共 {{ pageInfo.total || 0 }} 人</span>
      <button class="quiet" type="button" :disabled="page >= (pageInfo.totalPages || 1)" @click="changePage(page + 1)">下一页</button>
    </div>

    <Modal :open="creating" title="登记读者" @close="creating = false">
      <form @submit.prevent="create">
        <label class="field"><span>姓名</span><input v-model="createForm.username" required /></label>
        <label class="field"><span>邮箱</span><input v-model="createForm.email" required /></label>
        <label class="field"><span>电话</span><input v-model="createForm.phone" /></label>
        <button class="primary" type="submit">保存</button>
      </form>
    </Modal>
    <Modal :open="editing" title="修改读者" @close="editing = false">
      <form @submit.prevent="update">
        <label class="field"><span>邮箱</span><input v-model="editForm.email" required /></label>
        <label class="field"><span>电话</span><input v-model="editForm.phone" /></label>
        <label class="field">
          <span>状态</span>
          <select v-model="editForm.status">
            <option value="ACTIVE">可用</option>
            <option value="DISABLED">停用</option>
          </select>
        </label>
        <button class="primary" type="submit">保存</button>
      </form>
    </Modal>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import Modal from '../components/Modal.vue'
import { gql } from '../api/graphql'
import { membershipStatusLabel, planLabel, readerStatusLabel, when } from '../api/labels'

const readers = ref([])
const pageInfo = ref({})
const page = ref(1)
const keyword = ref('')
const status = ref('')
const error = ref('')
const creating = ref(false)
const editing = ref(false)
const confirmId = ref(null)
const openId = ref(null)
const detail = ref([])
const createForm = reactive({ username: '', email: '', phone: '' })
const editForm = reactive({ id: null, email: '', phone: '', status: 'ACTIVE' })

async function load() {
  error.value = ''
  confirmId.value = null
  try {
    const data = await gql(`query($filter: ReaderFilter, $page: PageQuery) {
      readers(filter: $filter, page: $page) {
        items { id username email phone status createdAt }
        pageInfo { page size total totalPages }
      }
    }`, {
      filter: { keyword: keyword.value || null, status: status.value || null },
      page: { page: page.value, size: 10, sortBy: 'createdAt', direction: 'DESC' }
    })
    readers.value = data.readers.items
    pageInfo.value = data.readers.pageInfo
  } catch (e) {
    error.value = e.message
  }
}

function reload() {
  page.value = 1
  openId.value = null
  load()
}

function changePage(next) {
  page.value = next
  openId.value = null
  load()
}

function openCreate() {
  Object.assign(createForm, { username: '', email: '', phone: '' })
  creating.value = true
}

function openEdit(reader) {
  Object.assign(editForm, {
    id: reader.id,
    email: reader.email,
    phone: reader.phone || '',
    status: reader.status
  })
  editing.value = true
}

async function create() {
  try {
    await gql(`mutation($input: ReaderInput!) { createReader(input: $input) { id } }`, {
      input: { username: createForm.username, email: createForm.email, phone: createForm.phone || null }
    })
    creating.value = false
    await load()
  } catch (e) {
    error.value = e.message
    creating.value = false
  }
}

async function update() {
  try {
    await gql(`mutation($input: ReaderUpdateInput!) { updateReader(input: $input) { id } }`, {
      input: { id: editForm.id, email: editForm.email, phone: editForm.phone, status: editForm.status }
    })
    editing.value = false
    await load()
  } catch (e) {
    error.value = e.message
    editing.value = false
  }
}

async function remove(reader) {
  try {
    await gql(`mutation($id: Int!) { deleteReader(id: $id) }`, { id: reader.id })
    await load()
  } catch (e) {
    error.value = e.message
  }
}

async function toggle(reader) {
  if (openId.value === reader.id) {
    openId.value = null
    return
  }
  try {
    const data = await gql(`query($readerId: Int!) {
      readerMemberships(readerId: $readerId) { id plan status book { name } }
    }`, { readerId: reader.id })
    detail.value = data.readerMemberships
    openId.value = reader.id
  } catch (e) {
    error.value = e.message
  }
}

onMounted(load)
</script>
