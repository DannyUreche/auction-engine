import { Component } from 'react'
import { reportError } from '../errorReporting.js'

// If a page crashes while rendering, show a friendly message instead of a blank screen and report the error
export default class ErrorBoundary extends Component {
  state = { failed: false }

  static getDerivedStateFromError() {
    return { failed: true }
  }

  componentDidCatch(error, info) {
    reportError(error, info?.componentStack ?? '')
  }

  render() {
    if (!this.state.failed) return this.props.children
    return (
      <div className="grid min-h-screen place-items-center bg-slate-50 p-6 text-center">
        <div className="max-w-md">
          <h1 className="text-2xl font-bold text-slate-900">Ceva nu a mers bine</h1>
          <p className="mt-2 text-slate-600">Am inregistrat eroarea si o vom rezolva. Reincarca pagina ca sa continui.</p>
          <button
            onClick={() => window.location.reload()}
            className="mt-6 rounded-xl bg-indigo-600 px-5 py-2.5 font-semibold text-white shadow-md hover:bg-indigo-500"
          >
            Reincarca pagina
          </button>
        </div>
      </div>
    )
  }
}
