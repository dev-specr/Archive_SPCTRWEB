import React, { createContext, useContext, useEffect, useState } from 'react';
import api from '../api/client';

type User = { id: number; username: string; roles: string[] } | null;

type AuthCtx = {
  user: User;
  token: string | null;
  setToken: (t: string | null) => void;
  loginUrl: string | null;
  features: Record<string, boolean>;
  logout: () => void;
  refreshMe: () => Promise<void>;
};

const Ctx = createContext<AuthCtx | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('accessToken'));
  const [user, setUser] = useState<User>(null);
  const [loginUrl, setLoginUrl] = useState<string | null>(null);
  const [features, setFeatures] = useState<Record<string, boolean>>({});

  const refreshMe = async () => {
    if (!token) { setUser(null); return; }
    try {
      const res = await api.get('/api/me');
      setUser({
        id: res.data.id,
        username: res.data.username,
        roles: res.data.roles || []
      });
    } catch {
      setUser(null);
    }
  };

  useEffect(() => {
    // Load config and login URL
    api.get('/api/public/config').then(r => {
      setLoginUrl(r.data.loginUrl);
      if (r.data.features) setFeatures(r.data.features);
    }).catch(() => {});
  }, []);

  useEffect(() => {
    if (token) localStorage.setItem('accessToken', token);
    else localStorage.removeItem('accessToken');
    refreshMe();
  }, [token]);

  const logout = () => {
    setToken(null);
    setUser(null);
  };

  return (
    <Ctx.Provider value={{ user, token, setToken, loginUrl, features, logout, refreshMe }}>
      {children}
    </Ctx.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useAuth outside AuthProvider');
  return ctx;
}

