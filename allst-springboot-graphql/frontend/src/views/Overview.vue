<template>
  <section>
    <div class="page-head">
      <div>
        <h1>今天的流通</h1>
        <p class="lede">在订的数量只统计状态为在订的记录。暂停、退订和到期另计。</p>
      </div>
    </div>
    <p v-if="error" class="banner">{{ error }}</p>
    <div v-if="stats" class="ledger">
      <div><span>书目</span><strong>{{ stats.bookCount }}</strong></div>
      <div><span>作者</span><strong>{{ stats.authorCount }}</strong></div>
      <div><span>读者</span><strong>{{ stats.readerCount }}</strong></div>
      <div><span>分类</span><strong>{{ stats.categoryCount }}</strong></div>
      <div><span>在订</span><strong>{{ stats.activeMembershipCount }}</strong></div>
    </div>
    <article v-if="latest" class="slip">
      <p class="meta">最近一笔</p>
      <p class="who">{{ latest.reader.username }}</p>
      <p>订了 {{ latest.book.name }}</p>
      <p>{{ planLabel[latest.plan] }}，{{ membershipStatusLabel[latest.status] }}，{{ money(latest.price) }}</p>
      <p class="meta">{{ latest.startDate }} 至 {{ latest.endDate }}</p>
    </article>
    <p v-else-if="!error" class="empty">今天还没有订阅。先到订阅页记一笔。</p>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { gql } from '../api/graphql'
import { membershipStatusLabel, money, planLabel } from '../api/labels'

const stats = ref(null)
const latest = ref(null)
const error = ref('')

onMounted(async () => {
  try {
    const data = await gql(`query {
      deskStats { bookCount authorCount readerCount categoryCount activeMembershipCount }
      memberships(page: { page: 1, size: 1, sortBy: "createdAt", direction: DESC }) {
        items { plan status price startDate endDate reader { username } book { name } }
      }
    }`)
    stats.value = data.deskStats
    latest.value = data.memberships.items[0] || null
  } catch (e) {
    error.value = e.message
  }
})
</script>
