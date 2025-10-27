import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../api/client';

export default function Ships() {
  const { data: names } = useQuery({
    queryKey: ['ship-names'],
    queryFn: async () => (await api.get<string[]>('/api/ships/names')).data
  });

  const [name, setName] = useState('');
  const { data: info, refetch, isFetching } = useQuery({
    enabled: false,
    queryKey: ['ship-info', name],
    queryFn: async () => (await api.get('/api/ships/info', { params: { name } })).data
  });

  const filtered = useMemo(() => {
    if (!names) return [] as string[];
    if (!name) return names.slice(0, 50);
    return names.filter(n => n.toLowerCase().includes(name.toLowerCase())).slice(0, 50);
  }, [names, name]);

  return (
    <div>
      <h2>Ships</h2>
      <div className="flex gap-2 items-center">
        <input className="w-64" placeholder="Search ship..." value={name} onChange={e => setName(e.target.value)} />
        <button onClick={() => refetch()} disabled={!name || isFetching}>Load</button>
      </div>
      {info && (
        <div className="mt-4 rounded-lg border border-gray-200 dark:border-neutral-800 p-4">
          <h3>{info.name}</h3>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-x-6 gap-y-1 text-sm mt-2">
            <div>Manufacturer: <span className="font-medium">{info.manufacturer || '-'}</span></div>
            <div>Type: <span className="font-medium">{info.type || '-'}</span></div>
            <div>Focus: <span className="font-medium">{info.focus || '-'}</span></div>
            <div>Size: <span className="font-medium">{info.size || '-'}</span></div>
            <div>Cargo: <span className="font-medium">{info.cargoCapacity ?? '-'}</span></div>
            <div>SCM/Max: <span className="font-medium">{info.scmSpeed ?? '-'} / {info.navMaxSpeed ?? '-'}</span></div>
          </div>
        </div>
      )}
      <div className="mt-4">
        <h4>Suggestions</h4>
        <div className="flex flex-wrap gap-2">
          {filtered.map(n => (
            <button key={n} onClick={() => setName(n)} className="bg-gray-100 dark:bg-neutral-800 text-gray-900 dark:text-neutral-100 hover:bg-gray-200 dark:hover:bg-neutral-700">{n}</button>
          ))}
        </div>
      </div>
    </div>
  );
}
