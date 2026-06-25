import perfectCrown from './images/drama-perfect-crown.jpg'
import kitchenSoldier from './images/drama-kitchen-soldier.jpg'
import tangerines from './images/drama-tangerines.jpg'
import moneyHeist from './images/drama-money-heist.jpg'
import sceneTangerines from './images/scene-tangerines.jpg'
import bagel from './images/place-bagel.png'

export const dramaFallbacks = [perfectCrown, kitchenSoldier, tangerines, moneyHeist]
export const sceneFallback = sceneTangerines
export const placeFallback = bagel

export const fallbackFor = (item, index = 0) => {
  if (item?.kind === 'scene') return sceneFallback
  if (item?.kind === 'place') return placeFallback
  return dramaFallbacks[index % dramaFallbacks.length]
}
