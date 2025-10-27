import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { hasRole } from '../utils/roles';
import React from 'react';

export default function PostEditor() {
  const { id } = useParams();
  const editing = Boolean(id);
  const nav = useNavigate();
  const qc = useQueryClient();
  const { user } = useAuth();
  const isAdmin = hasRole(user, 'ROLE_ADMIN');
  const canWrite = hasRole(user, 'ROLE_MEMBER') || isAdmin;

  const { data } = useQuery({
    enabled: editing,
    queryKey: ['post', id],
    queryFn: async () => (await api.get(`/api/posts/${id}`)).data
  });

  const [title, setTitle] = React.useState('');
  const [content, setContent] = React.useState('');

  React.useEffect(() => {
    if (data) {
      setTitle(data.title || '');
      setContent(data.content || '');
    }
  }, [data]);

  const create = useMutation({
    mutationFn: async (body: { title: string; content: string }) => (await api.post('/api/posts', body)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['posts'] }); nav('/posts'); }
  });

  const update = useMutation({
    mutationFn: async (body: { title: string; content: string }) => (await api.put(`/api/posts/${id}`, body)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['posts'] }); nav('/posts'); }
  });

  if (!canWrite) return <p className="text-red-600">You need MEMBER or ADMIN role to write posts.</p>;

  return (
    <div>
      <h2>{editing ? 'Edit Post' : 'New Post'}</h2>
      <form className="flex flex-col gap-3 max-w-2xl" onSubmit={e => {
        e.preventDefault();
        const body = { title: title.trim(), content: content.trim() };
        if (editing) update.mutate(body); else create.mutate(body);
      }}>
        <input placeholder="Title" value={title} onChange={e => setTitle(e.target.value)} />
        <textarea
          placeholder="Write your content..."
          value={content}
          onChange={e => setContent(e.target.value)}
          rows={10}
          className="rounded-md border border-neutral-700 bg-neutral-900 text-neutral-100 p-3 text-sm"
        />
        <div className="flex gap-2">
          <button disabled={!title.trim() || !content.trim()}>{editing ? 'Save' : 'Create'}</button>
          <button type="button" className="bg-gray-500 hover:bg-gray-600" onClick={() => nav('/posts')}>Cancel</button>
        </div>
        {(create.isError || update.isError) && (
          <p className="text-red-600 text-sm">{editing ? 'Update' : 'Create'} failed. Ensure you are logged in and have permissions.</p>
        )}
      </form>
    </div>
  );
}
