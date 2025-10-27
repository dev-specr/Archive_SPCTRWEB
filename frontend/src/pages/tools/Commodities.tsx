import { useQuery } from '@tanstack/react-query';
import React from 'react';
import api from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { useIsAdmin, useIsMember } from '../../utils/roles';

type Summary = {
  commodity: string;
  buyLocation: string;
  buyPrice: number;
  sellLocation: string;
  sellPrice: number;
  spread: number;
}

export default function Commodities() {
  const isMember = useIsMember();
  const isAdmin = useIsAdmin();
  const [q, setQ] = React.useState('');
  const [dq, setDq] = React.useState('');

  // Debounce user input to avoid firing a request per keystroke
  React.useEffect(() => {
    const t = setTimeout(() => setDq(q.trim()), 350);
    return () => clearTimeout(t);
  }, [q]);

  const { data, isFetching, refetch, error } = useQuery({
    enabled: Boolean(isMember || isAdmin),
    queryKey: ['commodities', dq],
    queryFn: async () => (await api.get<Summary[]>(`/api/tools/commodities${dq ? `?q=${encodeURIComponent(dq)}` : ''}`)).data,
    keepPreviousData: true,
    staleTime: 30_000,
  });

  if (!isMember && !isAdmin) return <p className="text-red-500">Members only.</p>;
  if (error) return <p className="text-red-500">Failed to load commodities.</p>;

  const rows = data || [];

  return (
    <div>
      <h2>Commodity Prices</h2>
      <div className="mb-4 flex gap-2 items-center">
        <input className="w-64" value={q} onChange={e => setQ(e.target.value)} placeholder="Search commodity..." />
        <button onClick={() => refetch()} disabled={isFetching}>Search</button>
      </div>
      <div className="overflow-x-auto rounded-xl border border-neutral-800">
        <table className="min-w-full text-sm">
          <thead className="bg-neutral-900">
            <tr>
              <th className="text-left p-3">Commodity</th>
              <th className="text-left p-3">Best Buy</th>
              <th className="text-left p-3">Buy Price</th>
              <th className="text-left p-3">Best Sell</th>
              <th className="text-left p-3">Sell Price</th>
              <th className="text-left p-3">Spread</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={i} className="border-t border-neutral-800">
                <td className="p-3 font-medium">{r.commodity}</td>
                <td className="p-3">{r.buyLocation}</td>
                <td className="p-3">{fmt(r.buyPrice)}</td>
                <td className="p-3">{r.sellLocation}</td>
                <td className="p-3">{fmt(r.sellPrice)}</td>
                <td className="p-3 text-green-400">{fmt(r.spread)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function fmt(n?: number) {
  if (n == null) return '-';
  return Intl.NumberFormat().format(n);
}
