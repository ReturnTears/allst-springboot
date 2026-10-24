<template>
  <div class="desk">
    <aside class="spines" aria-label="分区">
      <router-link to="/">总览</router-link>
      <router-link to="/books">书目</router-link>
      <router-link to="/authors">作者</router-link>
      <router-link to="/categories">分类</router-link>
      <router-link to="/readers">读者</router-link>
      <router-link to="/memberships">订阅</router-link>
    </aside>
    <div class="sheet">
      <header class="mast">
        <p class="brand">架阅</p>
        <p>订阅台</p>
      </header>
      <router-view />
    </div>
  </div>
</template>
