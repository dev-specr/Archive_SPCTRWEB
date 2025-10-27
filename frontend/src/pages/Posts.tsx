import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../api/client';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { hasRole } from '../utils/roles';

type Post = {
  id: number;
  title: string;
  content: string;
  userId: number;
  createdAt: string;
  updatedAt: string;
  mine: boolean;
};

export default function Posts() {
  const nav = useNavigate();
  const { user } = useAuth();
  const isAdmin = hasRole(user, 'ROLE_ADMIN');
  const canWrite = hasRole(user, 'ROLE_MEMBER') || isAdmin;
  const qc = useQueryClient();
  const { data } = useQuery({
    queryKey: ['posts'],
    queryFn: async () => (await api.get('/api/posts?size=20')).data
  });

  const items: Post[] = data?.content ?? [];

  const del = useMutation({
    mutationFn: async (id: number) => { await api.delete(`/api/posts/${id}`); },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['posts'] })
  });

  return (
    <div>
      <h2>Posts</h2>
      <div className="mb-4 flex items-center justify-between">
        <div className="text-sm text-neutral-400">Guild announcements and posts</div>
        {canWrite && <button onClick={() => nav('/posts/new')}>New Post</button>}
      </div>

      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {items.map((p) => {
          const date = p.createdAt ? new Date(p.createdAt) : null;
          const dateText = date ? date.toLocaleDateString() : '';
          const excerpt = p.content.length > 220 ? p.content.substring(0, 220) + '…' : p.content;
          return (
            <article key={p.id} className="relative rounded-xl p-5 bg-gradient-to-br from-neutral-900/70 to-neutral-900/30 border border-neutral-800 hover:border-red-600/50 transition">
              <div className="flex items-start justify-between gap-3">
                <h3 className="mb-2 text-lg font-semibold text-neutral-100">{p.title}</h3>
                <span className="text-[11px] px-2 py-0.5 rounded-full bg-neutral-800 text-neutral-300">{dateText}</span>
              </div>
              <p className="text-sm text-neutral-300 whitespace-pre-wrap leading-relaxed">{excerpt}</p>
              {(isAdmin || p.mine) && (
                <div className="mt-3 flex gap-2">
                  <Link to={`/posts/${p.id}/edit`} className="bg-neutral-800 hover:bg-neutral-700 text-white px-3 py-1.5 rounded-md text-sm">Edit</Link>
                  <button className="bg-red-600 hover:bg-red-700" onClick={() => del.mutate(p.id)}>Delete</button>
                </div>
              )}
            </article>
          );
        })}
      </div>
    </div>
  );
}
