import { useState } from 'react';
import api from '../api/client';

export default function Compare() {
  const [a, setA] = useState('');
  const [b, setB] = useState('');
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [aOptions, setAOptions] = useState<string[]>([]);
  const [bOptions, setBOptions] = useState<string[]>([]);

  const onCompare = async () => {
    setError(null);
    setResult(null);
    setAOptions([]);
    setBOptions([]);
    try {
      const resp = await api.post('/api/tools/compare', { a, b });
      setResult(resp.data);
    } catch (e: any) {
      const data = e?.response?.data;
      setError(data?.error || 'Compare failed. Login may be required.');
      if (data?.aCandidates) setAOptions(data.aCandidates);
      if (data?.bCandidates) setBOptions(data.bCandidates);
    }
  };

  return (
    <div>
      <h2>Compare Ships</h2>
      <div style={{display: 'flex', gap: 8}}>
        <input placeholder="Ship A" value={a} onChange={e => setA(e.target.value)} />
        <input placeholder="Ship B" value={b} onChange={e => setB(e.target.value)} />
        <button onClick={onCompare} disabled={!a || !b}>Compare</button>
      </div>
      {error && <p style={{color: 'red'}}>{error}</p>}
      {(aOptions.length > 0 || bOptions.length > 0) && (
        <div style={{marginTop: 8, display: 'flex', gap: 24}}>
          {aOptions.length > 0 && (
            <div>
              <div style={{fontWeight: 600}}>Options for A</div>
              <div style={{display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 6}}>
                {aOptions.map(opt => (
                  <button key={opt} onClick={() => setA(opt)}>{opt}</button>
                ))}
              </div>
            </div>
          )}
          {bOptions.length > 0 && (
            <div>
              <div style={{fontWeight: 600}}>Options for B</div>
              <div style={{display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 6}}>
                {bOptions.map(opt => (
                  <button key={opt} onClick={() => setB(opt)}>{opt}</button>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
      {result && (
        <pre style={{background: '#000000ff', padding: 12, marginTop: 12}}>
          {JSON.stringify(result, null, 2)}
        </pre>
      )}
    </div>
  );
}
