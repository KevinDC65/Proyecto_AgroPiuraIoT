# 06 · Diseño de interfaz

## Para quién se diseña

- **Agricultor pequeño o mediano de Piura** (mango, limón, arroz, banano). Revisa sus
  parcelas desde el celular en el campo, muchas veces a pleno sol, o desde una PC.
- **Administrador o colaborador de un cultivo:** el administrador configura umbrales y
  accesos; el colaborador consulta y atiende alertas.

Objetivo: que en pocos segundos se sepa **qué parcela está fuera de rango** y qué hacer.
Flujo principal: Dashboard → cultivo en alerta → umbrales o histórico → marcar la alerta
como atendida.

## Sistema visual "Chulucanas"

Inspirado en la cerámica de Chulucanas (Piura): fondo crema de arcilla cocida, negro
ahumado como tinta y arcilla como único color de acción. **Solo modo claro**, a propósito:
se lee mejor a pleno sol.

La fuente de verdad es `src/main/resources/static/css/app.css`: todos los valores están
como variables en `:root`. **No escribir colores, tamaños ni sombras sueltos**; usar las
variables.

### Paleta

| Rol | Variable | Valor |
|---|---|---|
| Fondo | `--crema` | `#f5efe4` |
| Superficie (paneles) | `--papel` | `#fffcf6` |
| Menú lateral, botón base | `--ahumado` | `#1f1a16` |
| Texto / texto secundario | `--tinta` / `--tinta-suave` | `#221c17` / `#6b6056` |
| Acción principal, foco | `--arcilla` | `#b5532a` |
| Estado normal / alerta / sin datos | `--ok` / `--alerta` / `--neutro` | `#2f6b3f` / `#b42318` / `#6b6056` |
| Series de gráficas | `--serie-1` a `--serie-4` | arcilla, verde, azul acero, ocre |

La arcilla es **acción**; el rojo es **estado de alerta**. Nunca se intercambian.
Todos los textos cumplen contraste WCAG AA (4.5:1), incluidos los textos de ejemplo de
los campos.

### Tipografía

- **Hanken Grotesk** (400–700) para toda la interfaz; cifras tabulares en datos.
- **Bricolage Grotesque** (600–700) solo para títulos de página, valores de indicadores
  y la portada.
- Escala: `--t-xs` 12px · `--t-sm` 13px · `--t-base` 15px · `--t-md` 17px ·
  `--t-lg` 21px · `--t-xl` 28px · `--t-2xl` 36px. Título de página `--t-2xl`
  (`--t-xl` en celular), título de panel `--t-lg`.
- Ambas fuentes se cargan de Google Fonts; sin internet se usa la fuente del sistema.

### Elemento propio: la banda de rango

Regla graduada (marcas cada 10 %) con una franja rayada de arcilla para el umbral mínimo–
máximo y una aguja para la lectura. Aparece en el dashboard (humedad por cultivo), en
umbrales, en el histórico (mínimo–máximo observado y promedio) y en la portada.

```html
<div class="banda" data-estado="alerta" style="--min:35;--max:80;--valor:28;--escala:100">
  <span class="banda-rango"></span><span class="banda-aguja"></span>
</div>
```

## Componentes (`app.css`)

| Componente | Clases | Notas |
|---|---|---|
| Botones | `.btn`, `.primario`, `.secundario`, `.fantasma`, `.peligro`; tamaños `.chico`, `.bloque` | Un solo `.primario` por pantalla. Al presionar escalan a 0.97; el hover solo con mouse |
| Estados | `.estado.ok`, `.alerta`, `.sin-datos`, `.admin` | Siempre forma + texto, nunca solo color (alerta = rombo, sin datos = círculo hueco) |
| Paneles | `.panel`, `.panel-cabeza`, `.panel-cuerpo` | — |
| Indicadores | `.metricas`, `.metrica`, `.metrica-valor`, `.metrica-nota` | Una franja; la de alertas se pone roja si hay pendientes |
| Tablas | `.tabla.apilable` + `data-etiqueta` en cada celda | En ≤720px se convierten en tarjetas |
| Formularios | `.form`, `.campo`, `.fila` | Error bajo cada campo; campos a 16px en celular para evitar zoom |
| Avisos | `.aviso.info`, `.aviso.ok`, `.aviso.error` | Con `role="status"` o `role="alert"` |
| Estado vacío | `.vacio` | Explica qué hacer y ofrece la acción |
| Iconos | `components/icons.html` | `<svg th:replace="~{components/icons :: icono('alerta')}"></svg>` |

## Plantillas

- `components/layout.html`: estructura común (cabecera, menú lateral, migas de pan,
  enlace "Saltar al contenido", contador de alertas, usuario y botón de salir). Todas las
  páginas lo usan.
- `components/icons.html`: iconos SVG en línea.
- Una carpeta por área: `cultivos/`, `umbrales/`, `alertas/`, `accesos/`, `historico/`,
  `mapa/`, `error/`.

## Diseño adaptable y celular

- Escritorio: menú lateral de 252px; contenido hasta 1320px. Rejilla principal + lateral
  que se apila en ≤1180px.
- ≤960px: el menú pasa a una barra superior con menú desplegable (se cierra con Escape).
- ≤720px: tablas en tarjetas, formularios a una columna, botones a todo el ancho.
- Sin desplazamiento horizontal a 390px; sin destello gris al tocar.

## Movimiento

Solo transiciones de estado de 160–240 ms con `--ease-out`. Animaciones: la entrada de la
portada (una vez) y el menú móvil (240 ms). Con `prefers-reduced-motion` todo queda quieto.

## Accesibilidad

- Enlace "Saltar al contenido" como primer elemento enfocable.
- Foco visible en todos los controles (anillo de arcilla).
- Etiquetas `<label>` en todos los campos y `autocomplete` en login y registro.
- Acciones destructivas con confirmación.
- Jerarquía de títulos sin saltos (`h1` → `h2` → `h3`).

## Librerías externas en la interfaz

| Librería | Origen | Uso |
|---|---|---|
| Chart.js 4.4.9 | `static/js/chart.umd.min.js` (local) | Gráfica del histórico |
| Leaflet 1.9.4 | unpkg.com | Mapa; sin internet se muestra un aviso |
| OpenStreetMap | tiles en línea | Fondo del mapa |
| Google Fonts | fonts.googleapis.com | Hanken Grotesk y Bricolage Grotesque |
