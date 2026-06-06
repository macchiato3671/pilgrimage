import { publicApiClient } from "./apiClient"

const fetchDramaList = (orderCond, key = null) => {
  return publicApiClient.get("/dramas", {
    params: {
      OrderCondition: orderCond,
      keyword: key
    }
  })
}

export { fetchDramaList }