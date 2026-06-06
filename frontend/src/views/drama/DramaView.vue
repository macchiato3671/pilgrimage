<template>
  <main>

    <aside>
      <h2>목차</h2>
      <nav>
        <ul>
          <li
            v-for="item in sidebarItems"
            :key="item"
          >
            <button @click="scrollToSection(item)">
              {{ item }}
            </button>
          </li>
        </ul>
      </nav>
    </aside>

    <section>
      <nav>
        <button @click="selectedFilter = 'year'">연도별</button>
        <span>|</span>
        <button @click="selectedFilter = 'genre'">장르별</button>
      </nav>

      <section
        v-for="group in groupedDramas"
        :key="group.name"
        :id="`section-${group.name}`"
      >
        <h2>{{ group.name }}</h2>
        <article
          v-for="drama in group.dramas"
          :key="drama.id"
        >
          <img
            :src="drama.posterURL"
            :alt="`${drama.title} 포스터`"
          />
          <h3>{{ drama.title }}</h3>
          <p>
            출시일: {{ drama.releasedAt }}
          </p>
          <p>
            장르:
            {{ drama.genres?.map((genre) => genre.name).join(', ') || '장르 정보 없음'}}
          </p>
          <p>
            {{ drama.description }}
          </p>
        </article> 
      </section>
    </section>
  </main>
</template>

<script setup>

</script>

<style lang="scss" scoped>

</style>