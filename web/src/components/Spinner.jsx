export default function Spinner() {
  return (
    <div style={styles.wrapper}>
      <div style={styles.circle} />
      <style>{`@keyframes mtmh-spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  )
}

const styles = {
  wrapper: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '100vh',
  },
  circle: {
    width: '36px',
    height: '36px',
    border: '3px solid #e5e7eb',
    borderTopColor: '#6366f1',
    borderRadius: '50%',
    animation: 'mtmh-spin 0.7s linear infinite',
  },
}
