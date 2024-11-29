import React, { useState, useEffect } from'react';
import './App.css';
import IncidentForm from './components/IncidentForm';
import IncidentTable from './components/IncidentTable';

function App() {
  const [incidents, setIncidents] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  // 获取所有事件的异步函数
  const fetchIncidents = async () => {
    try {
      const response = await fetch('http://localhost:8080/incident');
      if (response.ok) {
        const data = await response.json();
        setIncidents(data);
      }
      setIsLoading(false);
      setErrorMessage('');
    } catch (error) {
      console.error('Error fetching incidents:', error);
      setErrorMessage('加载事件列表出现错误，请稍后重试');
      setIsLoading(false);
    }
  };

  // 删除事件的异步函数
  const handleDeleteIncident = async (incidentId) => {
    try {
      const response = await fetch(`http://localhost:8080/incident/${incidentId}`, {
        method: 'DELETE',
      });
      if (response.ok) {
        setIncidents(prevIncidents => prevIncidents.filter(incident => incident.id!== incidentId));
      } else {
        setErrorMessage('删除事件失败，请稍后重试');
      }
    } catch (error) {
      console.error('Error deleting incident:', error);
      setErrorMessage('删除事件出现网络错误，请检查网络连接');
    }
  };

  useEffect(() => {
    fetchIncidents();
  }, []);

  return (
      <div className="App">
        <h1>事件管理应用</h1>
        <IncidentForm refreshIncidents={fetchIncidents} />
        {errorMessage && <p style={{ color:'red' }}>{errorMessage}</p>}
        {isLoading? (
            <p>正在加载事件列表...</p>
        ) : (
            <IncidentTable incidents={incidents} onDelete={handleDeleteIncident} />
        )}
      </div>
  );
}

export default App;