// Mapeo icono Material Icon → color, replicado de obtenerColorIcono() en CategoriaUI.kt (Android)
export const ICON_COLORS = {
  shopping_cart:    '#81C784', // Verde suave
  work:             '#90CAF9', // Azul claro
  home:             '#FFCC80', // Ámbar suave
  star:             '#FFD54F', // Amarillo apagado
  event:            '#F48FB1', // Rosa suave
  settings:         '#90A4AE', // Gris azulado suave
  person:           '#80DEEA', // Cian suave
  code:             '#CE93D8', // Lavanda / Morado suave
  lightbulb:        '#FFF176', // Amarillo muy suave
  restaurant:       '#FFAB91', // Salmón suave
  directions_car:   '#EF9A9A', // Rojo suave
  fitness_center:   '#80CBC4', // Turquesa suave
  payments:         '#A5D6A7', // Verde claro
  medical_services: '#EF9A9A', // Rojo suave (salud)
  school:           '#9FA8DA', // Índigo suave
  pet_page:         '#BCAAA4', // Marrón suave
  favorite:         '#F48FB1', // Rosa suave
  build:            '#B0BEC5', // Gris claro
  call:             '#A5D6A7', // Verde claro
  list:             '#9E9E9E', // Gris medio
}

export const ICON_COLOR_DEFAULT = '#757575'

export function getIconColor(iconName) {
  return ICON_COLORS[iconName] ?? ICON_COLOR_DEFAULT
}
