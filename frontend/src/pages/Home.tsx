import { useQuery } from '@tanstack/react-query';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';

type Person = { name: string };
const leaders: Person[] = [
  { name: 'I_CUCUMBER_I' },
  { name: 'SpaceKlaus' },
  { name: 'Chimaera' },
];
const admin: Person[] = [
  { name: 'Lorilord' },
];
const moderators: Person[] = [
  { name: 'F1tzZZ' },
  { name: 'N3onius' },
  { name: 'RAULSAMA' },
];



export default function Home() {
  const { user, loginUrl } = useAuth();
  const { data } = useQuery({
    queryKey: ['config'],
    queryFn: async () => (await api.get('/api/public/config')).data
  });

  const joinUrl = 'https://discord.com/invite/specr';

  const history = [
    {
      year: '2950',
      title: 'Spectre Founded',
      text: 'A small crew of freelancers band together to fly smarter and farther than anyone expects.'
    },
    {
      year: '2951',
      title: 'First Fleet Ops',
      text: 'Coordinated mining and escort missions prove the concept: discipline beats firepower.'
    },
    {
      year: '2952',
      title: 'Allied Accord',
      text: 'Spectre joins forces with another guild, expanding logistics and intel coverage across systems.'
    },
    {
      year: '2953',
      title: 'Signals Division',
      text: 'A dedicated recon wing begins mapping routes and markets for precision trade runs.'
    },
    {
      year: '2954',
      title: 'Beyond the Rim',
      text: 'Operations extend to new frontiers — Spectre remains lean, fast, and focused.'
    }
  ];

  return (
    <div>
      {/* Hero (full-bleed banner across viewport) */}
      <section className="relative overflow-hidden w-screen mx-[calc(50%-50vw)]">
        <div className="absolute inset-0">
          <img src="/brand/banner.png" alt="Spectre Banner" className="h-full w-full object-cover" />
          <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/40 to-transparent" />
        </div>
        <div className="container relative flex items-center min-h-[65vh] py-16">
          <div className="max-w-2xl">
            <h1 className="mb-4">Welcome to Spectre</h1>
            <p className="text-neutral-200 mb-6">
              Wort • Ehre • Loyalität • Freiheit Wir sind ein entspanntes Syndikat – kein Druck, kein Zwangsspiel, sondern echte Kameradschaft im Game. Überzeug dich selbst!.
            </p>
            <div className="flex gap-3">
              <a href={joinUrl} className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700">Join Discord</a>
              <a href="/ships" className="inline-flex items-center rounded-md border border-neutral-700 px-4 py-2 text-sm hover:bg-neutral-800">Entdecke Schiffe</a>
            </div>
          </div>
        </div>
      </section>

      {/* What we do */}
      <section className="section">
        <div className="container text-center">
          <h2 className="mb-6">Was wir machen</h2>
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 justify-items-center">
            {[
              { title: 'Mining', desc: 'Coordinated multi-ship mining ops with route planning.' },
              { title: 'Pirating', desc: 'High-risk engagements, intel, and profit extraction.' },
              { title: 'Trading', desc: 'Market analysis and best-routes optimization.' },
              { title: 'Security', desc: 'Escort and enforcement for Spectre operations.' },
            ].map((c) => (
              <div key={c.title} className="card max-w-sm">
                <h3 className="mb-1">{c.title}</h3>
                <p className="text-sm text-neutral-300">{c.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* History / Storyline */}
      <section className="section">
        <div className="container">
          <h2 className="mb-6 text-center">Unsere Geschichet</h2>

          {/* Vertical (mobile) */}
          <div className="md:hidden space-y-6">
            {history.map((h, i) => (
              <div key={i} className="card">
                <div className="text-sm text-neutral-400 mb-1">{h.year}</div>
                <div className="font-semibold">{h.title}</div>
                <p className="text-sm text-neutral-300 mt-1">{h.text}</p>
              </div>
            ))}
          </div>

          {/* Horizontal (md+) */}
          <div className="hidden md:block relative">
            <div className="grid grid-cols-5 gap-4">
              {history.map((h, i) => (
                <div key={i} className="relative text-center">
                  <div className="inline-flex items-center justify-center w-10 h-10 rounded-full bg-red-600 text-white font-bold shadow-md">
                    {i + 1}
                  </div>
                  <div className="mt-3 font-semibold">{h.title}</div>
                  <div className="text-sm text-neutral-400">{h.year}</div>
                  <p className="text-sm text-neutral-300 mt-1 px-2">{h.text}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Tools grid (3 per row on large) */}
      <section className="section">
        <div className="container text-center">
          <h2 className="mb-6">Tools</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 justify-items-center">
            {[
              { name: 'Ships', href: '/ships', desc: 'Find ship info quickly.' },
              { name: 'Images', href: '/images', desc: 'Share your best shots.' },
              { name: 'Posts', href: '/posts', desc: 'Guild announcements and posts.' },
              { name: 'Ship Compare', href: '/tools/compare', desc: 'Side-by-side stats (Members only)', locked: true },
              { name: 'Commodity Prices', href: '#', desc: 'Best buy/sell (Members only)', locked: true },
              { name: 'Earnings Calculator', href: '#', desc: 'Plan profits (Members only)', locked: true },
            ].map((t) => (
              <a key={t.name} href={t.href} className="card w-full max-w-sm text-center">
                <div className="flex flex-col items-center gap-2">
                  <h3 className="text-center">{t.name}</h3>
                  {t.locked && (
                    <span className="text-[10px] uppercase tracking-wide bg-red-600/90 text-white px-2 py-0.5 rounded-full">Members</span>
                  )}
                </div>
                <p className="text-sm text-neutral-300 mt-1">{t.desc}</p>
              </a>
            ))}
          </div>
        </div>
      </section>

      {/* Team hierarchy (3 leaders, 1 admin, 3 moderators) */}
      <section className="section">
        <div className="container text-center">
          <h2 className="mb-8">Team</h2>
          <div className="space-y-6">
            {/* Leaders row */}
            <div className="text-center">
              <div className="mb-4 text-sm uppercase tracking-wide text-neutral-400">Leaders</div>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-8 place-items-center">
                {leaders.map((p) => (
                  <div key={p.name} className="w-48 text-center">
                    <div className="text-lg md:text-xl font-semibold">{p.name}</div>
                  </div>
                ))}
              </div>
            </div>
            {/* Admin row */}
            <div className="text-center">
              <div className="mb-4 text-sm uppercase tracking-wide text-neutral-400">Admin</div>
              <div className="grid grid-cols-1 gap-8 place-items-center">
                {admin.map((p) => (
                  <div key={p.name} className="w-48 text-center">
                    <div className="text-lg md:text-xl font-semibold">{p.name}</div>
                  </div>
                ))}
              </div>
            </div>
            {/* Moderators row */}
            <div className="text-center">
              <div className="mb-4 text-sm uppercase tracking-wide text-neutral-400">Moderators</div>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-8 place-items-center">
                {moderators.map((p) => (
                  <div key={p.name} className="w-48 text-center">
                    <div className="text-lg md:text-xl font-semibold">{p.name}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="section">
        <div className="container text-center">
          <h2 className="mb-3">Join Spectre</h2>
          <p className="text-neutral-300 mb-5">Be part of focused operations with a disciplined crew.</p>
          <a className="inline-flex items-center rounded-md bg-red-600 px-6 py-3 text-base font-semibold text-white hover:bg-red-700" href={joinUrl}>
            Join Discord
          </a>
        </div>
      </section>
    </div>
  );
}
