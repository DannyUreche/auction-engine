import { useEffect, useRef, useState } from 'react'
import { checkImage, IMAGE_TYPES } from '../api.js'

// Alegerea unei imagini: click sau drag & drop, cu previzualizare
export default function ImagePicker({ file, onChange, onError }) {
  const inputRef = useRef(null)
  const [dragging, setDragging] = useState(false)
  const [preview, setPreview] = useState(null)

  // Previzualizarea se face local, inainte de upload
  useEffect(() => {
    if (!file) {
      setPreview(null)
      return
    }
    const url = URL.createObjectURL(file)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [file])

  function choose(selected) {
    if (!selected) return
    const problem = checkImage(selected)
    if (problem) {
      onError(problem)
      return
    }
    onError('')
    onChange(selected)
  }

  function handleDrop(event) {
    event.preventDefault()
    setDragging(false)
    choose(event.dataTransfer.files?.[0])
  }

  return (
    <div>
      <input
        ref={inputRef}
        type="file"
        accept={IMAGE_TYPES.join(',')}
        className="hidden"
        onChange={(e) => {
          choose(e.target.files?.[0])
          e.target.value = ''
        }}
      />

      {preview ? (
        <div className="relative overflow-hidden rounded-xl ring-1 ring-slate-200">
          <img src={preview} alt="Previzualizare" className="aspect-[4/3] w-full object-cover" />
          <div className="absolute right-3 bottom-3 flex gap-2">
            <button
              type="button"
              onClick={() => inputRef.current.click()}
              className="rounded-lg bg-white/90 px-3 py-1.5 text-sm font-medium text-slate-700 shadow-sm backdrop-blur hover:bg-white"
            >
              Schimba
            </button>
            <button
              type="button"
              onClick={() => onChange(null)}
              className="rounded-lg bg-white/90 px-3 py-1.5 text-sm font-medium text-red-600 shadow-sm backdrop-blur hover:bg-white"
            >
              Sterge
            </button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => inputRef.current.click()}
          onDragOver={(e) => {
            e.preventDefault()
            setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={handleDrop}
          className={`flex aspect-[4/3] w-full flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed transition ${
            dragging ? 'border-indigo-500 bg-indigo-50' : 'border-slate-300 bg-slate-50 hover:border-indigo-400 hover:bg-indigo-50/50'
          }`}
        >
          <span className="text-4xl">🖼️</span>
          <span className="text-sm font-semibold text-slate-700">Alege o imagine sau trage-o aici</span>
          <span className="text-xs text-slate-500">JPG, PNG, WebP sau GIF, maxim 5 MB</span>
        </button>
      )}
    </div>
  )
}
