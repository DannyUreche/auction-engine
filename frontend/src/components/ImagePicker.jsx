import { useEffect, useRef, useState } from 'react'
import { checkImage, IMAGE_TYPES, MAX_IMAGES } from '../api.js'
import { Icon } from './icons.jsx'

// Alegerea pozelor (maxim MAX_IMAGES): click sau drag & drop, cu previzualizare.
// Prima poza este poza principala (apare pe card); click pe alta o muta pe primul loc.
export default function ImagePicker({ files, onChange, onError }) {
  const inputRef = useRef(null)
  const [dragging, setDragging] = useState(false)
  const [previews, setPreviews] = useState([])

  // Previzualizarea se face local, inainte de upload
  useEffect(() => {
    const urls = files.map((file) => URL.createObjectURL(file))
    setPreviews(urls)
    return () => urls.forEach((url) => URL.revokeObjectURL(url))
  }, [files])

  function add(selected) {
    const list = [...(selected ?? [])]
    if (list.length === 0) return
    const problem = list.map(checkImage).find(Boolean)
    if (problem) {
      onError(problem)
      return
    }
    const room = MAX_IMAGES - files.length
    onError(list.length > room ? `Un obiect poate avea maxim ${MAX_IMAGES} poze!` : '')
    onChange([...files, ...list.slice(0, room)])
  }

  const remove = (index) => onChange(files.filter((_, i) => i !== index))
  const makeMain = (index) => onChange([files[index], ...files.filter((_, i) => i !== index)])

  const dropHandlers = {
    onDragOver: (e) => {
      e.preventDefault()
      setDragging(true)
    },
    onDragLeave: () => setDragging(false),
    onDrop: (e) => {
      e.preventDefault()
      setDragging(false)
      add(e.dataTransfer.files)
    },
  }
  const dropZone = dragging
    ? 'border-indigo-500 bg-indigo-50'
    : 'border-slate-300 bg-slate-50 hover:border-indigo-400 hover:bg-indigo-50/50'

  return (
    <div>
      <input
        ref={inputRef}
        type="file"
        multiple
        accept={IMAGE_TYPES.join(',')}
        className="hidden"
        onChange={(e) => {
          add(e.target.files)
          e.target.value = ''
        }}
      />

      {files.length === 0 ? (
        <button
          type="button"
          onClick={() => inputRef.current.click()}
          {...dropHandlers}
          className={`flex aspect-[4/3] w-full flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed transition ${dropZone}`}
        >
          <Icon name="image" size={46} />
          <span className="text-sm font-semibold text-slate-700">Alege pana la {MAX_IMAGES} poze sau trage-le aici</span>
          <span className="text-xs text-slate-500">JPG, PNG, WebP sau GIF, maxim 5 MB fiecare</span>
        </button>
      ) : (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-5">
          {previews.map((url, index) => (
            <div key={url} className="relative aspect-square overflow-hidden rounded-xl ring-1 ring-slate-200">
              <button
                type="button"
                onClick={() => makeMain(index)}
                disabled={index === 0}
                title={index === 0 ? 'Poza principala' : 'Fa-o poza principala'}
                className="block h-full w-full"
              >
                <img src={url} alt={`Poza ${index + 1}`} className="h-full w-full object-cover" />
              </button>
              {index === 0 && (
                <span className="absolute bottom-1.5 left-1.5 rounded-full bg-indigo-600 px-2 py-0.5 text-[10px] font-semibold text-white">
                  Principala
                </span>
              )}
              <button
                type="button"
                onClick={() => remove(index)}
                aria-label="Sterge poza"
                className="absolute top-1.5 right-1.5 grid h-6 w-6 place-items-center rounded-full bg-white/90 text-xs text-red-600 shadow-sm hover:bg-white"
              >
                ✕
              </button>
            </div>
          ))}
          {files.length < MAX_IMAGES && (
            <button
              type="button"
              onClick={() => inputRef.current.click()}
              {...dropHandlers}
              className={`flex aspect-square flex-col items-center justify-center gap-1 rounded-xl border-2 border-dashed text-slate-500 transition ${dropZone}`}
            >
              <span className="text-2xl leading-none">+</span>
              <span className="text-xs font-medium">
                Adauga ({files.length}/{MAX_IMAGES})
              </span>
            </button>
          )}
        </div>
      )}
    </div>
  )
}
