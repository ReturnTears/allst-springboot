<template>
  <section>
    <div class="page-head">
      <div>
        <h1>书目</h1>
        <p class="lede">有订阅的书不能删除。示例书如果只挂着作者，删除时作者会跟着原有外键一起去掉。</p>
      </div>
      <button class="primary" type="button" @click="openCreate">新书上架</button>
    </div>
    <p v-if="error" class="banner">{{ error }}</p>
    <div class="filters">
      <label class="field grow">
        <span>按书名</span>
        <input v-model="keyword" @keyup.enter="load" />
      </label>
      <button class="quiet" type="button" @click="load">查找</button>
    </div>
    <table>
      <thead>
        <tr><th>书名</th><th>页数</th><th>作者</th><th>分类</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="book in books" :key="book.id">
          <td>{{ book.name }}</td>
          <td>{{ book.pageCount }}</td>
          <td>{{ authorName(book.author) }}</td>
          <td>{{ book.category ? book.category.name : '未分类' }}</td>
          <td class="row-actions">
            <button class="quiet" type="button" @click="openEdit(book)">修改</button>
            <button class="quiet" type="button" @click="openCategory(book)">分类</button>
            <button v-if="confirmId !== book.id" class="danger" type="button" @click="confirmId = book.id">删除</button>
            <button v-else class="danger" type="button" @click="remove(book)">确认删除</button>
          </td>
        </tr>
        <tr v-if="!books.length"><td colspan="5" class="empty">没有匹配的书。</td></tr>
      </tbody>
    </table>

    <Modal :open="editing" :title="form.id ? '修改书目' : '新书上架'" @close="editing = false">
      <form @submit.prevent="saveBook">
        <label class="field"><span>书名</span><input v-model="form.name" required /></label>
        <label class="field"><span>页数</span><input v-model="form.pageCount" /></label>
        <button class="primary" type="submit">保存</button>
      </form>
    </Modal>

    <Modal :open="categorizing" title="归入分类" @close="categorizing = false">
      <form @submit.prevent="saveCategory">
        <label class="field">
          <span>{{ categoryForm.name }}</span>
          <select v-model="categoryForm.categoryId">
            <option value="">不分类</option>
            <option v-for="item in categories" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
          </select>
        </label>
        <button class="primary" type="submit">保存分类</button>
      </form>
    </Modal>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import Modal from '../components/Modal.vue'
import { gql } from '../api/graphql'

const books = ref([])
const categories = ref([])
const keyword = ref('')
const error = ref('')
const editing = ref(false)
const categorizing = ref(false)
const confirmId = ref(null)
const form = reactive({ id: null, name: '', pageCount: '' })
const categoryForm = reactive({ id: null, name: '', categoryId: '' })

function authorName(author) {
  if (!author) return '暂无'
  return `${author.firstName || ''} ${author.lastName || ''}`.trim()
}

async function load() {
  error.value = ''
  confirmId.value = null
  try {
    const data = await gql(`query($keyword: String) {
      searchBooks(keyword: $keyword) { id name pageCount author { firstName lastName } category { id name } }
      categories { id name }
    }`, { keyword: keyword.value || null })
    books.value = data.searchBooks
    categories.value = data.categories
  } catch (e) {
    error.value = e.message
  }
}

function openCreate() {
  Object.assign(form, { id: null, name: '', pageCount: '' })
  editing.value = true
}

function openEdit(book) {
  Object.assign(form, { id: book.id, name: book.name, pageCount: book.pageCount || '' })
  editing.value = true
}

function openCategory(book) {
  Object.assign(categoryForm, {
    id: book.id,
    name: book.name,
    categoryId: book.category ? String(book.category.id) : ''
  })
  categorizing.value = true
}

async function saveBook() {
  try {
    if (form.id) {
      await gql(`mutation($input: BookUpdateInput!) {
        updateBook(input: $input) { id }
      }`, { input: { id: form.id, name: form.name, pageCount: form.pageCount } })
    } else {
      await gql(`mutation($name: String!, $pageCount: String) {
        newBook(name: $name, pageCount: $pageCount) { id }
      }`, { name: form.name, pageCount: form.pageCount || null })
    }
    editing.value = false
    await load()
  } catch (e) {
    error.value = e.message
    editing.value = false
  }
}

async function saveCategory() {
  try {
    if (categoryForm.categoryId) {
      await gql(`mutation($bookId: Int!, $categoryId: Int!) {
        assignBookCategory(bookId: $bookId, categoryId: $categoryId) { id }
      }`, { bookId: categoryForm.id, categoryId: Number(categoryForm.categoryId) })
    } else {
      await gql(`mutation($bookId: Int!) { clearBookCategory(bookId: $bookId) }`, { bookId: categoryForm.id })
    }
    categorizing.value = false
    await load()
  } catch (e) {
    error.value = e.message
    categorizing.value = false
  }
}

async function remove(book) {
  try {
    await gql(`mutation($id: Int!) { deleteBook(id: $id) { id } }`, { id: book.id })
    await load()
  } catch (e) {
    error.value = e.message
  }
}

onMounted(load)
</script>
