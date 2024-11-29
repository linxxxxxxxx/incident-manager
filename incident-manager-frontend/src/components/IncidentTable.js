import React from'react';
import Button from'react-bootstrap/Button';
import Table from'react-bootstrap/Table';

function IncidentTable({ incidents, onDelete }) {
    return (
        <Table striped bordered hover>
            <thead>
            <tr>
                <th>ID</th>
                <th>名称</th>
                <th>描述</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            {incidents.map(incident => (
                <tr key={incident.id}>
                    <td>{incident.id}</td>
                    <td>{incident.name}</td>
                    <td>{incident.description}</td>
                    <td>
                        <Button variant="danger" onClick={() => onDelete(incident.id)}>删除</Button>
                    </td>
                </tr>
            ))}
            </tbody>
        </Table>
    );
}

export default IncidentTable;