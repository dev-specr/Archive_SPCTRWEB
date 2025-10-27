import React from 'react';
import { useMutation } from '@tanstack/react-query';
import api from '../../api/client';
import { useIsAdmin, useIsMember } from '../../utils/roles';

type Route = {
  commodity: string;
  buyLocation: string;
  buyPrice: number;
  sellLocation: string;
  sellPrice: number;
  spread: number;
  quantity: number;
  profit: number;
}

export default function Earnings() {
  const isMember = useIsMember();
  const isAdmin = useIsAdmin();
  const [topN, setTopN] = React.useState<number>(10);
  const [quantity, setQuantity] = React.useState<number>(1);
  const [system, setSystem] = React.useState('');
  const [allowCross, setAllowCross] = React.useState(false);
  const [results, setResults] = React.useState<Route[] | null>(null);

  const compute = useMutation({
    mutationFn: async () => {
      const payload: any = {
        topN,
        quantity,
        allowCrossSystem: allowCross,
      };
      if (system.trim()) payload.system = system.trim();
      const r = await api.post<Route[]>('/api/tools/routes', payload);
      return r.data;
    },
    onSuccess: (data) => setResults(data)
  });

  if (!isMember && !isAdmin) return <p className="text-red-500">Members only.</p>;

  return (
    <div>
      <h2>Earnings Calculator</h2>
      <form className="grid grid-cols-1 md:grid-cols-5 gap-3 mb-4" onSubmit={e => { e.preventDefault(); compute.mutate(); }}>
        <div>
          <label className="block text-xs mb-1 opacity-80">Top N</label>
          <input type="number" min={1} max={100} value={topN} onChange={e => setTopN(Number(e.target.value))} />
        </div>
        <div>
          <label className="block text-xs mb-1 opacity-80">Quantity (units)</label>
          <input type="number" min={1} value={quantity} onChange={e => setQuantity(Number(e.target.value))} />
        </div>
        <div>
          <label className="block text-xs mb-1 opacity-80">System (optional)</label>
          <input placeholder="e.g., Stanton" value={system} onChange={e => setSystem(e.target.value)} />
        </div>
        <div className="flex items-end gap-2">
          <input id="cross" type="checkbox" checked={allowCross} onChange={e => setAllowCross(e.target.checked)} />
          <label htmlFor="cross" className="text-sm">Allow cross-system</label>
        </div>
        <div className="flex items-end">
          <button type="submit" disabled={compute.isPending}>{compute.isPending ? 'Calculating…' : 'Calculate'}</button>
        </div>
      </form>

      {results && (
        <div className="overflow-x-auto rounded-xl border border-neutral-800">
          <table className="min-w-full text-sm">
            <thead className="bg-neutral-900">
              <tr>
                <th className="text-left p-3">Commodity</th>
                <th className="text-left p-3">Buy</th>
                <th className="text-left p-3">Buy Price</th>
                <th className="text-left p-3">Sell</th>
                <th className="text-left p-3">Sell Price</th>
                <th className="text-left p-3">Spread</th>
                <th className="text-left p-3">Qty</th>
                <th className="text-left p-3">Profit</th>
              </tr>
            </thead>
            <tbody>
              {results.map((r, i) => (
                <tr key={i} className="border-top border-neutral-800">
                  <td className="p-3 font-medium">{r.commodity}</td>
                  <td className="p-3">{r.buyLocation}</td>
                  <td className="p-3">{fmt(r.buyPrice)}</td>
                  <td className="p-3">{r.sellLocation}</td>
                  <td className="p-3">{fmt(r.sellPrice)}</td>
                  <td className="p-3">{fmt(r.spread)}</td>
                  <td className="p-3">{fmt(r.quantity)}</td>
                  <td className="p-3 text-green-400">{fmt(r.profit)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function fmt(n?: number) {
  if (n == null) return '-';
  return Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(n);
}

