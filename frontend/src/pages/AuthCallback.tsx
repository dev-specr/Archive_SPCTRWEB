import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function AuthCallback() {
  const params = new URLSearchParams(useLocation().search);
  const code = params.get('code');
  const state = params.get('state');
  const nav = useNavigate();
  const { setToken, refreshMe } = useAuth();

  // Guard against React StrictMode double-invocation of effects in dev
  const ran = useRef(false);
  useEffect(() => {
    if (ran.current) return;
    ran.current = true;
    (async () => {
      try {
        if (!code) { nav('/'); return; }
        const resp = await api.get(`/api/auth/discord/callback?code=${encodeURIComponent(code)}${state ? `&state=${encodeURIComponent(state)}` : ''}`);
        if (resp.data?.accessToken) {
          setToken(resp.data.accessToken);
          await refreshMe();
        }
      } catch (e) {
        console.error('Auth callback failed', e);
      } finally {
        nav('/');
      }
    })();
  }, []);

  return <p>Completing login...</p>;
}
