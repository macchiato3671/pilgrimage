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
        <button @click="handleFilter('year')">연도별</button>
        <span>|</span>
        <button @click="handleFilter('genre')">장르별</button>
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
            :src="drama.posterUrl"
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
import { fetchDramaList } from '@/api/dramaApi';
import { computed, onMounted, ref } from 'vue';

const orderCondition = ref("YEAR");

const errorMessage = ref("");
const isLoading = ref(false);

const rawData = ref([]);
const years = computed(() => {
  const yearSet = new Set();

  rawData.value.forEach((drama) => {
    yearSet.add(new Date(drama.releasedAt).getFullYear());
  });

  return [...yearSet].sort((a, b) => b - a);
});
const genres = computed(() => {
  const genreSet = new Set();

  rawData.value.forEach((drama) => {
    drama.genres?.forEach((genre) => {
      genreSet.add(genre.name);
    });
  });

  return [...genreSet].sort((a, b) => a.localeCompare(b, 'ko'));
});
const sidebarItems = computed(() => {
  if (orderCondition.value.trim().toUpperCase() === "YEAR")
    return years.value;

  if (orderCondition.value.trim().toUpperCase() === "GENRE")
    return genres.value;

  return [];
});
const groupedDramas = computed(() => {
  return sidebarItems.value.map((sidebarItem) => {
    const dramasInGroup = rawData.value.filter((drama) => {
      if (orderCondition.value.trim().toUpperCase() === "YEAR")
        return new Date(drama.releasedAt).getFullYear() === sidebarItem;

      if (orderCondition.value.trim().toUpperCase() === "GENRE")
        return drama.genres?.some((genre) => genre.name === sidebarItem);

      return false;
    });

    return {
      name: sidebarItem,
      dramas: dramasInGroup,
    };
  });
});

const scrollToSection = (item) => {
  const target = document.getElementById(`section-${item}`);

  if (!target) return;

  target.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  });
};

const fetchData = async () => {
  errorMessage.value = "";

  try {
    // TODO: connect search keyword data after the header is implemented
    const response = await fetchDramaList(orderCondition.value, null);
    rawData.value = response.dramas
  }
  catch (error) {
    console.error("드라마 리스트 불러오기 실패" + error);
    errorMessage.value = "드라마 리스트를 불러오지 못했습니다."
  }
  finally {
    isLoading.value = false;
  }
};

const handleFilter = (filterValue) => {
  orderCondition.value = filterValue.trim().toUpperCase();
  fetchData();
};

onMounted(() => {
  fetchData();
});

</script>

<style lang="scss" scoped>

</style>