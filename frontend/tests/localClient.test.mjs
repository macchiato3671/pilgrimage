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
    })

    const created = await localApiClient.get('/wishlist')
    assert.equal(created.wishlists.length, 1)
    assert.equal(created.wishlists[0].scene.sceneId, '1')
    assert.equal(created.wishlists[0].scene.name, 'scene one')

    await localApiClient.delete('/wishlist/1')

    const deleted = await localApiClient.get('/wishlist')
    assert.deepEqual(deleted.wishlists, [])
  })

  it('creates, reads, updates, and deletes plans', async () => {
    const plan = await localApiClient.post('/plans', {
      title: 'trip',
      beginDate: '2026-06-10',
      endDate: '2026-06-11',
    })

    assert.ok(plan.planId)
    assert.equal(plan.title, 'trip')

    const list = await localApiClient.get('/plans')
    assert.equal(list.plans.length, 1)

    const detail = await localApiClient.get(`/plans/${plan.planId}`)
    assert.equal(detail.plan.title, 'trip')

    await localApiClient.put(`/plans/${plan.planId}`, {
      ...plan,
      title: 'updated trip',
    })

    const updated = await localApiClient.get(`/plans/${plan.planId}`)
    assert.equal(updated.plan.title, 'updated trip')

    await localApiClient.delete(`/plans/${plan.planId}`)

    const deleted = await localApiClient.get('/plans')
    assert.deepEqual(deleted.plans, [])
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
