<template>
  <section>
    <div class="page-head">
      <div>
        <h1>作者</h1>
        <p class="lede">这里沿用原来的作者修改接口。姓和名会一起写回。</p>
      </div>
    </div>
    <p v-if="error" class="banner">{{ error }}</p>
    <table>
      <thead>
        <tr><th>名</th><th>姓</th><th>图书编号</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="author in authors" :key="author.id">
          <td>{{ author.firstName }}</td>
          <td>{{ author.lastName }}</td>
          <td>{{ author.bookId }}</td>
          <td><button class="quiet" type="button" @click="openEdit(author)">修改</button></td>
        </tr>
      </tbody>
    </table>
    <Modal :open="editing" title="修改作者" @close="editing = false">
      <form @submit.prevent="save">
        <label class="field"><span>名</span><input v-model="form.firstName" required /></label>
        <label class="field"><span>姓</span><input v-model="form.lastName" /></label>
        <button class="primary" type="submit">保存</button>
      </form>
    </Modal>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import Modal from '../components/Modal.vue'
import { gql } from '../api/graphql'

const authors = ref([])
const error = ref('')
const editing = ref(false)
const form = reactive({ id: null, firstName: '', lastName: '' })

async function load() {
  error.value = ''
  try {
    const data = await gql(`query { allAuthor { id firstName lastName bookId } }`)
    authors.value = data.allAuthor
  } catch (e) {
    error.value = e.message
  }
}

function openEdit(author) {
  Object.assign(form, author)
  editing.value = true
}

async function save() {
  try {
    await gql(`mutation($id: Int!, $firstName: String, $lastName: String) {
      updateAuthorById(id: $id, firstName: $firstName, lastName: $lastName) { id }
    }`, { id: form.id, firstName: form.firstName, lastName: form.lastName })
    editing.value = false
    await load()
  } catch (e) {
    error.value = e.message
    editing.value = false
  }
}

onMounted(load)
</script>
