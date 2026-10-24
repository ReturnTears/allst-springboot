<template>
  <section>
    <div class="page-head">
      <div>
        <h1>订阅</h1>
        <p class="lede">{{ liveText }}</p>
      </div>
      <button class="primary" type="button" @click="openCreate">记一笔订阅</button>
    </div>
    <p v-if="error" class="banner">{{ error }}</p>
    <p v-if="notice" class="lede">{{ notice }}</p>
    <div class="filters">
      <label class="field grow"><span>书名或备注</span><input v-model="keyword" @keyup.enter="reload" /></label>
      <label class="field">
        <span>状态</span>
        <select v-model="status">
          <option value="">全部</option>
          <option v-for="(label, key) in membershipStatusLabel" :key="key" :value="key">{{ label }}</option>
        </select>
      </label>
      <label class="field">
        <span>方案</span>
        <select v-model="plan">
          <option value="">全部</option>
          <option v-for="quote in quotes" :key="quote.plan" :value="quote.plan">{{ planLabel[quote.plan] }}</option>
        </select>
      </label>
      <label class="field">
        <span>排序</span>
        <select v-model="sortBy">
          <option value="createdAt">最近登记</option>
          <option value="startDate">开始日期</option>
          <option value="price">价格</option>
        </select>
      </label>
      <button class="quiet" type="button" @click="reload">筛选</button>
    </div>
    <table>
      <thead>
        <tr><th>读者</th><th>书</th><th>方案</th><th>状态</th><th>起止</th><th>价格</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>{{ item.reader.username }}</td>
          <td>{{ item.book.name }}</td>
          <td>{{ planLabel[item.plan] }}</td>
          <td>{{ membershipStatusLabel[item.status] }}</td>
          <td>{{ item.startDate }} 至 {{ item.endDate }}</td>
          <td>{{ money(item.price) }}</td>
          <td class="row-actions">
            <button class="quiet" type="button" @click="openEdit(item)">修改</button>
            <button class="quiet" type="button" :disabled="item.status === 'CANCELLED'" @click="cancel(item)">退订</button>
            <button v-if="confirmId !== item.id" class="danger" type="button" @click="confirmId = item.id">删除</button>
            <button v-else class="danger" type="button" @click="remove(item)">确认删除</button>
          </td>
        </tr>
        <tr v-if="!items.length"><td colspan="7" class="empty">没有匹配的订阅。</td></tr>
      </tbody>
    </table>
    <div class="pager">
      <button class="quiet" type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span>第 {{ pageInfo.page || page }} / {{ pageInfo.totalPages || 1 }} 页，共 {{ pageInfo.total || 0 }} 笔</span>
      <button class="quiet" type="button" :disabled="page >= (pageInfo.totalPages || 1)" @click="changePage(page + 1)">下一页</button>
    </div>

    <Modal :open="creating" title="记一笔订阅" @close="creating = false">
      <form @submit.prevent="create">
        <label class="field">
          <span>读者</span>
          <select v-model="createForm.readerId" required>
            <option disabled value="">选择读者</option>
            <option v-for="reader in readers" :key="reader.id" :value="reader.id">{{ reader.username }}</option>
          </select>
        </label>
        <label class="field">
          <span>书</span>
          <select v-model="createForm.bookId" required>
            <option disabled value="">选择书</option>
            <option v-for="book in books" :key="book.id" :value="book.id">{{ book.name }}</option>
          </select>
        </label>
        <label class="field">
          <span>方案</span>
          <select v-model="createForm.plan" required>
            <option v-for="quote in quotes" :key="quote.plan" :value="quote.plan">
              {{ planLabel[quote.plan] }}，{{ quote.days }} 天，{{ money(quote.price) }}
            </option>
          </select>
        </label>
        <label class="field"><span>开始日期</span><input v-model="createForm.startDate" type="date" /></label>
        <label class="field"><span>备注</span><input v-model="createForm.remark" /></label>
        <button class="primary" type="submit">保存</button>
      </form>
    </Modal>

    <Modal :open="editing" title="修改订阅" @close="editing = false">
      <form @submit.prevent="update">
        <label class="field">
          <span>方案</span>
          <select v-model="editForm.plan">
            <option v-for="quote in quotes" :key="quote.plan" :value="quote.plan">{{ planLabel[quote.plan] }}</option>
          </select>
        </label>
        <label class="field">
          <span>状态</span>
          <select v-model="editForm.status">
            <option v-for="(label, key) in membershipStatusLabel" :key="key" :value="key">{{ label }}</option>
          </select>
        </label>
        <label class="field"><span>备注</span><input v-model="editForm.remark" /></label>
        <button class="primary" type="submit">保存</button>
      </form>
    </Modal>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import Modal from '../components/Modal.vue'
import { gql, subscribeMemberships } from '../api/graphql'
import { membershipStatusLabel, money, planLabel } from '../api/labels'

const items = ref([])
const pageInfo = ref({})
const page = ref(1)
const keyword = ref('')
const status = ref('')
const plan = ref('')
const sortBy = ref('createdAt')
const quotes = ref([])
const readers = ref([])
const books = ref([])
const error = ref('')
const notice = ref('')
const live = ref('closed')
const creating = ref(false)
const editing = ref(false)
const confirmId = ref(null)
const createForm = reactive({ readerId: '', bookId: '', plan: 'MONTHLY', startDate: '', remark: '' })
const editForm = reactive({ id: null, plan: 'MONTHLY', status: 'ACTIVE', remark: '' })
let stopSubscription = () => {}

const liveText = computed(() => {
  if (live.value === 'open') return '实时通道已连上。别人改一笔订阅，这页会自己刷新。'
  if (live.value === 'error') return '实时通道出错，列表仍可手动刷新。'
  return '实时通道未连接。'
})

async function load() {
  error.value = ''
  confirmId.value = null
  const data = await gql(`query($filter: MembershipFilter, $page: PageQuery) {
    memberships(filter: $filter, page: $page) {
      items { id plan status price startDate endDate remark reader { username } book { name } }
      pageInfo { page size total totalPages }
    }
    planQuotes { plan days price }
    readers(page: { page: 1, size: 50 }) { items { id username status } }
    findAll { id name }
  }`, {
    filter: {
      keyword: keyword.value || null,
      status: status.value || null,
      plan: plan.value || null
    },
    page: { page: page.value, size: 8, sortBy: sortBy.value, direction: 'DESC' }
  })
  items.value = data.memberships.items
  pageInfo.value = data.memberships.pageInfo
  quotes.value = data.planQuotes
  readers.value = data.readers.items.filter((reader) => reader.status === 'ACTIVE')
  books.value = data.findAll
}

async function safeLoad() {
  try {
    await load()
  } catch (e) {
    error.value = e.message
  }
}

function reload() {
  page.value = 1
  safeLoad()
}

function changePage(next) {
  page.value = next
  safeLoad()
}

function openCreate() {
  Object.assign(createForm, { readerId: '', bookId: '', plan: quotes.value[0]?.plan || 'MONTHLY', startDate: '', remark: '' })
  creating.value = true
}

function openEdit(item) {
  Object.assign(editForm, { id: item.id, plan: item.plan, status: item.status, remark: item.remark || '' })
  editing.value = true
}

async function create() {
  try {
    await gql(`mutation($input: MembershipInput!) { createMembership(input: $input) { id } }`, {
      input: {
        readerId: Number(createForm.readerId),
        bookId: Number(createForm.bookId),
        plan: createForm.plan,
        startDate: createForm.startDate || null,
        remark: createForm.remark || null
      }
    })
    creating.value = false
    await safeLoad()
  } catch (e) {
    error.value = e.message
    creating.value = false
  }
}

async function update() {
  try {
    await gql(`mutation($input: MembershipUpdateInput!) { updateMembership(input: $input) { id } }`, {
      input: { id: editForm.id, plan: editForm.plan, status: editForm.status, remark: editForm.remark }
    })
    editing.value = false
    await safeLoad()
  } catch (e) {
    error.value = e.message
    editing.value = false
  }
}

async function cancel(item) {
  try {
    await gql(`mutation($id: Int!) { cancelMembership(id: $id) { id status } }`, { id: item.id })
    await safeLoad()
  } catch (e) {
    error.value = e.message
  }
}

async function remove(item) {
  try {
    await gql(`mutation($id: Int!) { deleteMembership(id: $id) }`, { id: item.id })
    await safeLoad()
  } catch (e) {
    error.value = e.message
  }
}

onMounted(async () => {
  await safeLoad()
  stopSubscription = subscribeMemberships((changed) => {
    if (!changed) return
    const reader = changed.reader ? changed.reader.username : '读者'
    const book = changed.book ? changed.book.name : '书'
    notice.value = `${reader} 的《${book}》有变动。`
    safeLoad()
  }, (status) => {
    live.value = status
  })
})

onBeforeUnmount(() => stopSubscription())
</script>
