import { useState, useEffect, useRef } from 'react'

const ATAJOS = ['09:00', '12:00', '14:00', '18:00', '20:00']
const MINUTOS = ['00', '15', '30', '45']

function parseTime(str) {
  if (!str) return null
  const m = str.match(/^(\d{1,2}):(\d{2})$/)
  if (!m) return null
  const h = parseInt(m[1], 10), mn = parseInt(m[2], 10)
  if (h < 0 || h > 23 || mn < 0 || mn > 59) return null
  return { h, m: mn }
}

function fmt(h, m) {
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

export default function TimePicker({ id, value, onChange, disabled }) {
  const [open, setOpen]   = useState(false)
  const [draft, setDraft] = useState(value || '')
  const [selH, setSelH]   = useState(null)
  const [selM, setSelM]   = useState(null)
  const wrapRef  = useRef(null)
  const inputRef = useRef(null)

  useEffect(() => {
    setDraft(value || '')
    const p = parseTime(value)
    if (p) { setSelH(p.h); setSelM(p.m) }
    else   { setSelH(null); setSelM(null) }
  }, [value])

  // Close on outside click
  useEffect(() => {
    if (!open) return
    const handler = (e) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) {
        commitDraft()
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [open, draft])

  function commitDraft(raw) {
    let v = (raw ?? draft).trim()
    if (/^\d{4}$/.test(v)) v = v.slice(0, 2) + ':' + v.slice(2)
    if (/^\d{1,2}$/.test(v)) v = v.padStart(2, '0') + ':00'
    const p = parseTime(v)
    if (p) {
      const t = fmt(p.h, p.m)
      setDraft(t); setSelH(p.h); setSelM(p.m); onChange(t)
    } else if (!v) {
      setDraft(''); setSelH(null); setSelM(null); onChange('')
    } else {
      setDraft(value || '') // invalid — reset
    }
  }

  function handleInputChange(e) {
    let v = e.target.value.replace(/[^\d:]/g, '')
    if (v.length > 5) return
    // Auto-insert colon after 2 digits if no colon yet
    if (/^\d{2}$/.test(v) && !draft.includes(':')) v = v + ':'
    setDraft(v)
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter') {
      e.preventDefault()
      commitDraft()
      setOpen(false)
    } else if (e.key === 'Tab') {
      commitDraft()
      setOpen(false)
    } else if (e.key === 'Escape') {
      setDraft(value || '')
      setOpen(false)
      inputRef.current?.blur()
    } else if ((e.key === 'ArrowDown' || e.key === 'ArrowUp') && !open) {
      setOpen(true)
    }
  }

  function pickHour(h) {
    const m = selM ?? 0
    setSelH(h)
    const t = fmt(h, m)
    setDraft(t); onChange(t)
  }

  function pickMinute(m) {
    const h = selH ?? 12
    const mi = parseInt(m, 10)
    setSelM(mi)
    const t = fmt(h, mi)
    setDraft(t); onChange(t)
  }

  function pickAtajo(t) {
    const p = parseTime(t)
    if (!p) return
    setSelH(p.h); setSelM(p.m); setDraft(t); onChange(t)
    setOpen(false)
  }

  function clear() {
    setDraft(''); setSelH(null); setSelM(null); onChange('')
    setOpen(false)
  }

  const selMStr = selM !== null ? String(selM).padStart(2, '0') : null

  return (
    <div className="tp-wrap" ref={wrapRef}>
      {/* Input row */}
      <div className={`tp-row${disabled ? ' tp-disabled' : ''}${open ? ' tp-focused' : ''}`}>
        <span className="material-icons tp-icon" style={{ color: value ? '#6366f1' : '#9ca3af' }}>
          schedule
        </span>
        <input
          ref={inputRef}
          id={id}
          type="text"
          className="tp-input"
          value={draft}
          placeholder="Sin hora"
          disabled={disabled}
          autoComplete="off"
          inputMode="numeric"
          onChange={handleInputChange}
          onFocus={() => { if (!disabled) setOpen(true) }}
          onKeyDown={handleKeyDown}
          onBlur={() => {
            // Delay so popover clicks fire before blur cleanup
            setTimeout(() => {
              if (!wrapRef.current?.contains(document.activeElement)) {
                commitDraft()
                setOpen(false)
              }
            }, 160)
          }}
        />
        {value && !disabled && (
          <button type="button" className="tp-x" onClick={clear} tabIndex={-1} aria-label="Quitar hora">
            <span className="material-icons" style={{ fontSize: 14 }}>close</span>
          </button>
        )}
      </div>

      {/* Popover */}
      {open && !disabled && (
        <div className="tp-pop" role="dialog" aria-label="Selector de hora">

          {/* Atajos rápidos */}
          <p className="tp-pop-label">Atajos</p>
          <div className="tp-atajos">
            {ATAJOS.map(t => (
              <button key={t} type="button"
                className={`tp-atajo${value === t ? ' active' : ''}`}
                onMouseDown={(e) => { e.preventDefault(); pickAtajo(t) }}>
                {t}
              </button>
            ))}
          </div>

          <div className="tp-sep" />

          {/* Horas */}
          <p className="tp-pop-label">Hora</p>
          <div className="tp-hours">
            {Array.from({ length: 24 }, (_, i) => (
              <button key={i} type="button"
                className={`tp-hbtn${selH === i ? ' active' : ''}`}
                onMouseDown={(e) => { e.preventDefault(); pickHour(i) }}>
                {String(i).padStart(2, '0')}
              </button>
            ))}
          </div>

          <div className="tp-sep" />

          {/* Minutos */}
          <p className="tp-pop-label">Minutos</p>
          <div className="tp-mins">
            {MINUTOS.map(m => (
              <button key={m} type="button"
                className={`tp-mbtn${selMStr === m ? ' active' : ''}`}
                onMouseDown={(e) => { e.preventDefault(); pickMinute(m) }}>
                :{m}
              </button>
            ))}
          </div>

          <div className="tp-sep" />

          <button type="button" className="tp-clear"
            onMouseDown={(e) => { e.preventDefault(); clear() }}>
            Quitar hora
          </button>
        </div>
      )}
    </div>
  )
}
