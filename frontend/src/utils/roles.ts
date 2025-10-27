import { useAuth } from '../context/AuthContext';

export function hasRole(user: { roles: string[] } | null, role: string) {
  if (!user) return false;
  return (user.roles || []).map(r => r.toUpperCase()).includes(role.toUpperCase());
}

export function useIsAdmin() {
  const { user } = useAuth();
  return hasRole(user, 'ROLE_ADMIN');
}

export function useIsMember() {
  const { user } = useAuth();
  return hasRole(user, 'ROLE_MEMBER') || hasRole(user, 'ROLE_ADMIN');
}

