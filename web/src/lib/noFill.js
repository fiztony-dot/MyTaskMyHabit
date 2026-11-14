/**
 * Atributos para suprimir el autocompletado de gestores de contraseñas
 * en campos que no son de login.
 * Uso: <input {...NO_FILL} ... />  o  <form {...NO_FILL_FORM} ...>
 */
export const NO_FILL = {
  autoComplete: 'off',
  'data-dashlane-ignore': 'true',
  'data-lpignore': 'true',
  'data-1p-ignore': 'true',
  'data-bwignore': 'true',
}

// Solo autoComplete para elementos de formulario (form, select, textarea)
// donde los data-* attrs no son necesarios pero la convención se mantiene uniforme.
export const NO_FILL_FORM = { autoComplete: 'off' }
