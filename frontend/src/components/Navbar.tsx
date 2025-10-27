import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import { hasRole } from '../utils/roles';

function hasCompareRole(user: { roles: string[] } | null) {
  if (!user) return false;
  const rs = (user.roles || []).map(r => r.toUpperCase());
  return rs.includes('ROLE_MEMBER') || rs.includes('ROLE_ADMIN');
}

export default function Navbar() {
  const { user, loginUrl, logout } = useAuth();
  const nav = useNavigate();

  const handleLogin = async () => {
    try {
      // Prefer backend auth endpoint to mint a fresh state
      const resp = await api.get('/api/auth/discord/login');
      const url = resp.data.url || loginUrl;
      if (url) { window.location.href = url; return; }
      // Fallback to config-derived URL
      const cfg = await api.get('/api/public/config');
      const alt = cfg.data.loginUrl || loginUrl;
      if (alt) window.location.href = alt;
    } catch {
      if (loginUrl) window.location.href = loginUrl;
    }
  };

  const handleLogout = () => {
    logout();
    nav('/');
  };

  return (
    <header className="sticky top-0 z-50 py-3 md:py-4 px-4 md:px-6 bg-neutral-950/80 backdrop-blur">
      <nav className="flex items-center gap-6 md:gap-8 max-w-7xl mx-auto text-[15px] md:text-base font-semibold leading-none">
        <Link to="/" aria-label="Home" className="flex items-center">
          <img src="/brand/logo.png" alt="Spectre" className="h-12 w-12 md:h-16 md:w-16 lg:h-20 lg:w-20 object-contain" />
        </Link>
        <Link to="/ships">Ships</Link>
        <Link to="/images">Images</Link>
        <Link to="/posts">Posts</Link>
        {hasCompareRole(user) && <Link to="/tools/compare">Compare</Link>}
        {hasRole(user, 'ROLE_MEMBER') && <Link to="/tools/commodities">Commodities</Link>}
        {hasRole(user, 'ROLE_MEMBER') && <Link to="/tools/earnings">Earnings</Link>}
        {hasRole(user, 'ROLE_ADMIN') && <Link to="/admin/users">Admin</Link>}
        <div className="ml-auto">
          {user ? (
            <span className="inline-flex items-center gap-2">
              <span className="text-sm text-gray-600 dark:text-neutral-300">{user.username}</span>
              <button onClick={handleLogout}>Logout</button>
            </span>
          ) : (
            <button onClick={handleLogin}>Login mit Discord</button>
          )}
        </div>
      </nav>
    </header>
  );
}
