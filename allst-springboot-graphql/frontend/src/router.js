import Overview from './views/Overview.vue'
import Books from './views/Books.vue'
import Authors from './views/Authors.vue'
import Categories from './views/Categories.vue'
import Readers from './views/Readers.vue'
import Memberships from './views/Memberships.vue'

export default [
  { path: '/', name: 'overview', component: Overview, meta: { title: '今天的流通' } },
  { path: '/books', name: 'books', component: Books, meta: { title: '书目' } },
  { path: '/authors', name: 'authors', component: Authors, meta: { title: '作者' } },
  { path: '/categories', name: 'categories', component: Categories, meta: { title: '分类' } },
  { path: '/readers', name: 'readers', component: Readers, meta: { title: '读者' } },
  { path: '/memberships', name: 'memberships', component: Memberships, meta: { title: '订阅' } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]
