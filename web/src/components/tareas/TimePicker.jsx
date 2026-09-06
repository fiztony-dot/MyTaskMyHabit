import { useRef, useEffect } from 'react'

const ITEM_H   = 44          // px per item
const PAD_H    = ITEM_H * 2  // 88px — 2 invisible items above/below
const DRUM_H   = ITEM_H * 5  // 220px — 5 visible items

const HORAS   = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'))
const MINUTOS = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0'))

function Drum({ items, initIdx, innerRef }) {
  useEffect(() => {
    const el = innerRef.current
    if (el) el.scrollTop = initIdx * ITEM_H
  }, [])

  return (
    <div className="tp-drum" ref={innerRef}>
      <div className="tp-drum-pad" />
      {items.map((v) => (
        <div key={v} className="tp-item">{v}</div>
      ))}
      <div className="tp-drum-pad" />
    </div>
  )
}

export default function TimePicker({ value, onChange, onClose }) {
  const now   = new Date()
  const initH = value ? parseInt(value.split(':')[0], 10) : now.getHours()
  const initM = value ? parseInt(value.split(':')[1], 10) : now.getMinutes()

  const hRef = useRef(null)
  const mRef = useRef(null)

  function snapIdx(el, count) {
    return Math.min(Math.max(Math.round(el.scrollTop / ITEM_H), 0), count - 1)
  }

  function handleOk() {
    const h = snapIdx(hRef.current, 24)
    const m = snapIdx(mRef.current, 60)
    onChange(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`)
    onClose()
  }

  function handleClear() {
    onChange('')
    onClose()
  }

  return (
    <div className="tp-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="tp-panel" role="dialog" aria-modal="true" aria-label="Seleccionar hora">
        <p className="tp-heading">Hora límite</p>

        <div className="tp-drums">
          <div className="tp-col">
            <span className="tp-col-label">hora</span>
            <div className="tp-drum-wrap">
              <Drum items={HORAS}   initIdx={initH} innerRef={hRef} />
              <div className="tp-mask-top"    />
              <div className="tp-mask-bottom" />
              <div className="tp-highlight"   />
            </div>
          </div>

          <span className="tp-sep">:</span>

          <div className="tp-col">
            <span className="tp-col-label">min</span>
            <div className="tp-drum-wrap">
              <Drum items={MINUTOS} initIdx={initM} innerRef={mRef} />
              <div className="tp-mask-top"    />
              <div className="tp-mask-bottom" />
              <div className="tp-highlight"   />
            </div>
          </div>
        </div>

        <div className="tp-footer">
          <button type="button" className="tp-btn-clear" onClick={handleClear}>Sin hora</button>
          <button type="button" className="tp-btn-ok"    onClick={handleOk}>Aceptar</button>
        </div>
      </div>
    </div>
  )
}
