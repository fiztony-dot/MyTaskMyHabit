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

// Versiones saturadas/intensas (Material Design 700–900) para cabeceros de sección
export const ICON_COLORS_SATURATED = {
  shopping_cart:    '#388E3C', // Verde 700
  work:             '#1565C0', // Azul 800
  home:             '#E65100', // Deep Orange 900
  star:             '#F57F17', // Ámbar 900
  event:            '#AD1457', // Rosa 800
  settings:         '#37474F', // Blue Grey 800
  person:           '#006064', // Cian 900
  code:             '#6A1B9A', // Morado 900
  lightbulb:        '#F57F17', // Ámbar 900
  restaurant:       '#BF360C', // Deep Orange 900
  directions_car:   '#B71C1C', // Rojo 900
  fitness_center:   '#00695C', // Teal 800
  payments:         '#2E7D32', // Verde 800
  medical_services: '#B71C1C', // Rojo 900
  school:           '#283593', // Índigo 900
  pet_page:         '#4E342E', // Marrón 800
  favorite:         '#AD1457', // Rosa 800
  build:            '#455A64', // Blue Grey 700
  call:             '#2E7D32', // Verde 800
  list:             '#424242', // Gris 800
}

export const ICON_COLOR_DEFAULT          = '#757575'
export const ICON_COLOR_SATURATED_DEFAULT = '#374151'

export function getIconColor(iconName) {
  return ICON_COLORS[iconName] ?? ICON_COLOR_DEFAULT
}

export function getIconColorSaturated(iconName) {
  return ICON_COLORS_SATURATED[iconName] ?? ICON_COLOR_SATURATED_DEFAULT
}
