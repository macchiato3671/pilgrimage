const getLocalWishlists = () => JSON.parse(localStorage.getItem('wishlists')) || []
const getLocalPlans = () => JSON.parse(localStorage.getItem('plans')) || {}

export const localGetWishlists = () => {
  const wishlists = getLocalWishlists()
  return { wishlists }
}
export const localPostWishlists = (item) => {
  const wishlists = getLocalWishlists()
  const exists = wishlists.some(wishlist => {
    return String(wishlist.wishlistId) === String(item.wishlistId)
      || String(wishlist.scene?.sceneId) === String(item.scene?.sceneId)
  })
  if (exists) return item
  wishlists.push(item)
  localStorage.setItem('wishlists', JSON.stringify(wishlists))
  return item
}
export const localDeleteWishlists = (sceneId) => {
  const wishlists = getLocalWishlists()
  const nextWishlists = wishlists.filter((wishlist) => {
    return String(wishlist.scene?.sceneId) !== String(sceneId)
  })
  localStorage.setItem('wishlists', JSON.stringify(nextWishlists))
  return { wishlists: nextWishlists }
}

export const localPostPlan = (body) => {
  const plans = getLocalPlans()
  const exists = plans[body.planId] !== undefined
  if (exists) return body
  plans[body.planId] = body
  localStorage.setItem('plans', JSON.stringify(plans))
  return body
}
export const localGetPlans = () => {
  const planDict = getLocalPlans()
  const plans = Object.values(planDict)
  return { plans }
}
export const localGetPlan = (id) => {
  const plans = getLocalPlans()
  const plan = plans[id]
  return { plan }
}
export const localPutPlan = (id, body) => {
  const plans = getLocalPlans()
  plans[id] = body
  localStorage.setItem('plans', JSON.stringify(plans))
  return body
}
export const localDeletePlan = (id) => {
  const plans = getLocalPlans()
  delete plans[id]
  localStorage.setItem('plans', JSON.stringify(plans))
  return {}
}
