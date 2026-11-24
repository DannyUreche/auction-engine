import DotGridBackground from '../backgrounds/DotGridBackground.jsx'

// Animated background behind every page: soft floating color blobs and the magnetic dot grid,
// which reacts to the cursor, clicks and live bids. Fixed behind the page and never blocks clicks
// (the grid listens to the cursor on window).
export default function Backdrop() {
  return (
    <div aria-hidden="true" className="pointer-events-none fixed inset-0 -z-10 overflow-hidden bg-slate-50">
      <div className="absolute -top-40 -left-32 h-[34rem] w-[34rem] animate-float rounded-full bg-indigo-300/30 blur-3xl" />
      <div className="absolute top-1/3 -right-40 h-[30rem] w-[30rem] animate-float-slow rounded-full bg-fuchsia-300/25 blur-3xl" />
      <div className="absolute -bottom-48 left-1/4 h-[32rem] w-[32rem] animate-float rounded-full bg-sky-300/25 blur-3xl [animation-delay:-9s]" />
      <DotGridBackground />
    </div>
  )
}
