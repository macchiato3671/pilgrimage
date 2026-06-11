import assert from 'node:assert/strict'
import { beforeEach, describe, it } from 'node:test'

const store = new Map()

globalThis.localStorage = {
  getItem: (key) => store.get(key) ?? null,
  setItem: (key, value) => store.set(key, String(value)),
  removeItem: (key) => store.delete(key),
  clear: () => store.clear(),
}

const { localApiClient } = await import('../src/api/localClient.js')
const {
  localDeleteWishlists,
  localGetWishlists,
  localPostWishlists,
} = await import('../src/utils/localStorageUtil.js')

describe('localApiClient', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('adds, reads, and deletes wishlist by sceneId', async () => {
    await localApiClient.post('/wishlist/1', {
      name: 'scene one',
      address: 'seoul',
      latitude: 37.5665,
      longitude: 126.978,
    })

    const created = await localApiClient.get('/wishlist')
    assert.equal(created.wishlists.length, 1)
    assert.equal(created.wishlists[0].scene.sceneId, '1')
    assert.equal(created.wishlists[0].scene.name, 'scene one')

    const removed = await localApiClient.delete('/wishlist/1')
    assert.deepEqual(removed, { sceneId: '1', deleted: true })

    const deleted = await localApiClient.get('/wishlist')
    assert.deepEqual(deleted.wishlists, [])
  })

  it('creates, reads, updates, and deletes plans', async () => {
    const plan = await localApiClient.post('/plans', {
      title: 'trip',
      beginDate: '2026-06-10',
      endDate: '2026-06-11',
      details: [],
    })

    assert.ok(plan.planId)
    assert.equal(plan.title, 'trip')

    const list = await localApiClient.get('/plans')
    assert.equal(list.plans.length, 1)

    const detail = await localApiClient.get(`/plans/${plan.planId}`)
    assert.equal(detail.title, 'trip')

    await localApiClient.put(`/plans/${plan.planId}`, {
      ...plan,
      title: 'updated trip',
    })

    const updated = await localApiClient.get(`/plans/${plan.planId}`)
    assert.equal(updated.title, 'updated trip')

    const removed = await localApiClient.delete(`/plans/${plan.planId}`)
    assert.deepEqual(removed, { planId: plan.planId, deleted: true })

    const deleted = await localApiClient.get('/plans')
    assert.deepEqual(deleted.plans, [])
  })

  it('returns existing wishlist when adding a duplicate scene', async () => {
    const first = await localApiClient.post('/wishlist/1', {
      name: 'scene one',
      address: 'seoul',
      latitude: 37.5665,
      longitude: 126.978,
    })

    const second = await localApiClient.post('/wishlist/1', {
      name: 'scene one',
      address: 'seoul',
      latitude: 37.5665,
      longitude: 126.978,
    })

    assert.deepEqual(second, first)

    const response = await localApiClient.get('/wishlist')
    assert.equal(response.wishlists.length, 1)
  })

  it('throws normalized not found errors for missing local resources', async () => {
    await assert.rejects(
      localApiClient.get('/plans/missing'),
      {
        status: 404,
        errorCode: 'TRAVEL_PLAN_NOT_FOUND',
        data: {
          detail: {
            errorCode: 'TRAVEL_PLAN_NOT_FOUND',
            message: 'Travel plan not found.',
          },
        },
      },
    )

    await assert.rejects(
      localApiClient.delete('/wishlist/missing'),
      {
        status: 404,
        errorCode: 'WISHLIST_NOT_FOUND',
        data: {
          detail: {
            errorCode: 'WISHLIST_NOT_FOUND',
            message: 'Wishlist item not found.',
          },
        },
      },
    )
  })
})

describe('localStorageUtil', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('deletes wishlist using sceneId', () => {
    localPostWishlists({
      wishlistId: 'wishlist-1',
      scene: { sceneId: 1, name: 'scene one' },
    })

    localDeleteWishlists(1)

    assert.deepEqual(localGetWishlists().wishlists, [])
  })
})
