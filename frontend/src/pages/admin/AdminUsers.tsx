import { useQuery } from '@tanstack/react-query';
import api from '../../api/client';
import { useIsAdmin } from '../../utils/roles';
import React from 'react';

type UserSummary = {
  id: number;
  discordId: string;
  username: string;
  roles: string[];
};

export default function AdminUsers() {
  const isAdmin = useIsAdmin();
  const [busy, setBusy] = React.useState<string | null>(null);
  const [message, setMessage] = React.useState<string | null>(null);
  const [testOut, setTestOut] = React.useState<any>(null);
  const [bestOut, setBestOut] = React.useState<any>(null);
  const { data, isError } = useQuery({
    enabled: isAdmin,
    queryKey: ['admin-users'],
    queryFn: async () => (await api.get<UserSummary[]>('/api/admin/users')).data,
  });

  if (!isAdmin) return <p className="text-red-600">Admins only.</p>;
  if (isError) return <p className="text-red-600">Failed to load users.</p>;

  const users = data || [];

  return (
    <div>
      <h2>Admin</h2>
      <div className="mb-6 flex flex-wrap items-center gap-3">
        <button
          className="rounded-md bg-neutral-900 text-white px-3 py-2 border border-neutral-700 disabled:opacity-60"
          disabled={!!busy}
          onClick={async () => {
            setMessage(null); setBusy('ships');
            try {
              const r = await api.post<string>('/api/ships/admin/sync');
              setMessage(r.data || 'Ship sync complete');
            } catch (e: any) {
              setMessage(e?.response?.data?.error || 'Ship sync failed');
            } finally { setBusy(null); }
          }}
        >{busy === 'ships' ? 'Syncing ships...' : 'Sync Ships (SC Wiki)'}</button>

        <button
          className="rounded-md bg-neutral-900 text-white px-3 py-2 border border-neutral-700 disabled:opacity-60"
          disabled={!!busy}
          onClick={async () => {
            setMessage(null); setBusy('commodities');
            try {
              const r = await api.post<{ upserts: number }>('/api/admin/commodities/refresh');
              setMessage(`Commodities refreshed: ${r.data?.upserts ?? '?'} upserts`);
            } catch (e: any) {
              setMessage(e?.response?.data?.error || 'Commodities refresh failed');
            } finally { setBusy(null); }
          }}
        >{busy === 'commodities' ? 'Refreshing commodities...' : 'Refresh Commodities (UEX)'}</button>

        {message && <span className="text-sm opacity-80">{message}</span>}

        <button
          className="rounded-md bg-neutral-900 text-white px-3 py-2 border border-neutral-700 disabled:opacity-60"
          disabled={!!busy}
          onClick={async () => {
            setMessage(null); setBusy('test'); setTestOut(null);
            try {
              const r = await api.get('/api/admin/commodities/test?id=1'); // Agricium
              setTestOut(r.data);
              setMessage('UEX test OK');
            } catch (e: any) {
              setMessage(e?.response?.data?.error || 'UEX test failed');
            } finally { setBusy(null); }
          }}
        >{busy === 'test' ? 'Testing UEX...' : 'Test UEX (Agricium)'}</button>

        <button
          className="rounded-md bg-neutral-900 text-white px-3 py-2 border border-neutral-700 disabled:opacity-60"
          disabled={!!busy}
          onClick={async () => {
            setMessage(null); setBusy('catalog');
            try {
              const r = await api.post<{ upserts: number }>('/api/admin/commodities/catalog/refresh');
              setMessage(`Catalog refreshed: ${r.data?.upserts ?? '?'} upserts`);
            } catch (e: any) {
              setMessage(e?.response?.data?.error || 'Catalog refresh failed');
            } finally { setBusy(null); }
          }}
        >{busy === 'catalog' ? 'Refreshing catalog...' : 'Refresh Catalog (UEX)'}</button>

        <button
          className="rounded-md bg-neutral-900 text-white px-3 py-2 border border-neutral-700 disabled:opacity-60"
          disabled={!!busy}
          onClick={async () => {
            setMessage(null); setBusy('best'); setBestOut(null);
            try {
              const r = await api.get('/api/tools/commodities/best?name=Agricium');
              setBestOut(r.data);
              setMessage('Best from DB OK');
            } catch (e: any) {
              setMessage(e?.response?.data?.error || 'Best from DB failed');
            } finally { setBusy(null); }
          }}
        >{busy === 'best' ? 'Checking DB...' : 'Best from DB (Agricium)'}</button>
      </div>

      {testOut && (
        <pre className="text-xs bg-neutral-900 text-neutral-100 p-3 rounded-md overflow-x-auto max-h-80">
          {JSON.stringify(testOut, null, 2)}
        </pre>
      )}

      {bestOut && (
        <pre className="text-xs bg-neutral-900 text-neutral-100 p-3 rounded-md overflow-x-auto max-h-80 mt-3">
          {JSON.stringify(bestOut, null, 2)}
        </pre>
      )}
      <div className="overflow-x-auto">
        <table className="min-w-full border border-neutral-800 rounded-lg overflow-hidden">
          <thead className="bg-neutral-900">
            <tr>
              <th className="text-left p-2 text-neutral-200">ID</th>
              <th className="text-left p-2 text-neutral-200">Discord ID</th>
              <th className="text-left p-2 text-neutral-200">Username</th>
              <th className="text-left p-2 text-neutral-200">Roles</th>
              <th className="text-left p-2 text-neutral-200">On Discord</th>
            </tr>
          </thead>
          <tbody>
            {users.map(u => {
              const roles = (u.roles || []).sort();
              const onDiscord = roles.includes('ROLE_MEMBER');
              return (
                <tr key={u.id} className="border-t border-neutral-800">
                  <td className="p-2">{u.id}</td>
                  <td className="p-2 font-mono text-xs opacity-80">{u.discordId || '-'}</td>
                  <td className="p-2">{u.username || '-'}</td>
                  <td className="p-2 text-xs">{roles.join(', ') || '-'}</td>
                  <td className="p-2">{onDiscord ? 'Yes' : 'No'}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <p className="mt-3 text-sm opacity-70">Note: "On Discord" reflects presence of ROLE_MEMBER (maintained by guild recheck).</p>
    </div>
  );
}
