import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import React from 'react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useIsAdmin } from '../utils/roles';

type ImageItem = {
  id: number;
  title: string;
  url: string;
  uploaderId: number;
  uploaderName?: string;
  uploadedAt: string;
  mine: boolean;
};

export default function Images() {
  const { user } = useAuth();
  const isAdmin = useIsAdmin();
  const qc = useQueryClient();
  const { data } = useQuery({
    queryKey: ['images'],
    queryFn: async () => (await api.get('/api/images?size=30')).data
  });

  const items: ImageItem[] = data?.content ?? [];

  const upload = useMutation({
    mutationFn: async (payload: { file: File; title?: string }) => {
      const fd = new FormData();
      fd.append('file', payload.file);
      if (payload.title) fd.append('title', payload.title);
      const r = await api.post('/api/images', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
      return r.data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['images'] })
  });

  const del = useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/api/images/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['images'] })
  });

  const [lightboxIndex, setLightboxIndex] = React.useState<number | null>(null);
  const closeLightbox = () => setLightboxIndex(null);
  const openLightbox = (idx: number) => setLightboxIndex(idx);
  const showPrev = () => setLightboxIndex(i => (i === null ? null : (i + items.length - 1) % items.length));
  const showNext = () => setLightboxIndex(i => (i === null ? null : (i + 1) % items.length));

  React.useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (lightboxIndex === null) return;
      if (e.key === 'Escape') closeLightbox();
      if (e.key === 'ArrowLeft') showPrev();
      if (e.key === 'ArrowRight') showNext();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [lightboxIndex, items.length]);

  return (
    <div>
      <h2>Images</h2>
      {user && (
        <div className="mb-4 p-4 border border-gray-200 dark:border-neutral-800 rounded-lg">
          <h4 className="mb-2">Upload Image</h4>
          <UploadForm onSubmit={(file, title) => upload.mutate({ file, title })} loading={upload.isPending} />
          {upload.isError && <p className="text-red-600 mt-2 text-sm">Upload failed. Ensure you are logged in.</p>}
        </div>
      )}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {items.map((img: ImageItem, idx: number) => (
          <figure
            key={img.id}
            className="relative group overflow-hidden rounded-xl cursor-zoom-in"
            onClick={() => openLightbox(idx)}
          >
            <div className="aspect-[16/9] w-full relative">
              <img
                src={img.url}
                alt={img.title}
                className="absolute inset-0 h-full w-full object-cover transform-gpu transition-transform duration-300 ease-out group-hover:scale-[1.04]"
              />
              <div className="absolute inset-x-0 bottom-0 h-1/2 bg-gradient-to-t from-black/60 to-transparent" />
              <div className="absolute inset-x-0 bottom-0 z-10 flex items-end justify-between p-3 text-xs pointer-events-none">
                <span className="font-medium text-white drop-shadow">{img.title}</span>
                <span className="text-white/90 drop-shadow">{img.uploaderName || `Uploader #${img.uploaderId}`}</span>
              </div>
              {(img.mine || isAdmin) && (
                <div className="absolute top-2 right-2 z-10" onClick={e => e.stopPropagation()}>
                  <button className="bg-red-600/90 hover:bg-red-700" onClick={() => del.mutate(img.id)}>Delete</button>
                </div>
              )}
            </div>
          </figure>
        ))}
      </div>

      {lightboxIndex !== null && items[lightboxIndex] && (
        <div
          className="fixed inset-0 z-50 bg-black/90 backdrop-blur-sm flex items-center justify-center"
          onClick={closeLightbox}
          role="dialog"
          aria-modal="true"
        >
          <div className="absolute top-3 right-3">
            <button className="bg-neutral-800 hover:bg-neutral-700" onClick={(e) => { e.stopPropagation(); closeLightbox(); }}>Close</button>
          </div>
          <div className="absolute left-3">
            <button className="bg-neutral-800 hover:bg-neutral-700" onClick={(e) => { e.stopPropagation(); showPrev(); }}>&larr;</button>
          </div>
          <div className="absolute right-3">
            <button className="bg-neutral-800 hover:bg-neutral-700" onClick={(e) => { e.stopPropagation(); showNext(); }}>&rarr;</button>
          </div>
          <div className="max-w-[92vw] max-h-[90vh] p-2" onClick={e => e.stopPropagation()}>
            <img
              src={items[lightboxIndex].url}
              alt={items[lightboxIndex].title}
              className="max-h-[86vh] max-w-[92vw] object-contain rounded-lg"
            />
            <div className="mt-2 text-center text-sm text-neutral-200">
              <span className="font-medium">{items[lightboxIndex].title}</span>
              <span className="opacity-70"> • {items[lightboxIndex].uploaderName || `Uploader #${items[lightboxIndex].uploaderId}`}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function UploadForm({ onSubmit, loading }: { onSubmit: (file: File, title?: string) => void; loading: boolean }) {
  const [title, setTitle] = React.useState('');
  const [file, setFile] = React.useState<File | null>(null);
  return (
    <form className="flex flex-col gap-2" onSubmit={e => { e.preventDefault(); if (file) onSubmit(file, title || undefined); }}>
      <input placeholder="Title (optional)" value={title} onChange={e => setTitle(e.target.value)} />
      <input type="file" accept="image/jpeg,image/png" onChange={e => setFile(e.target.files?.[0] || null)} />
      <div>
        <button disabled={!file || loading}>{loading ? 'Uploading…' : 'Upload'}</button>
      </div>
    </form>
  );
}
