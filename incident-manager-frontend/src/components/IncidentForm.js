import React, { useState } from'react';
import Button from'react-bootstrap/Button';
import Form from'react-bootstrap/Form';

function IncidentForm({ refreshIncidents }) {
    const [isEditMode, setIsEditMode] = useState(false);
    const [incidentIdToEdit, setIncidentIdToEdit] = useState(null);
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [nameError, setNameError] = useState('');
    const [descriptionError, setDescriptionError] = useState('');

    // 验证事件名称输入是否合法
    const validateName = () => {
        if (!name.trim()) {
            setNameError('名称不能为空');
            return false;
        } else if (name.length < 1 || name.length > 50) {
            setNameError('名称长度应在1到50个字符之间');
            return false;
        }
        setNameError('');
        return true;
    };

    // 验证事件描述输入是否合法
    const validateDescription = () => {
        if (!description.trim()) {
            setDescriptionError('描述不能为空');
            return false;
        } else if (description.length < 1 || description.length > 200) {
            setDescriptionError('描述长度应在1到200个字符之间');
            return false;
        }
        setDescriptionError('');
        return true;
    };

    // 处理表单提交的异步函数
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!validateName() ||!validateDescription()) {
            return;
        }

        const incidentData = {
            id: incidentIdToEdit,
            name,
            description,
            dateTime: new Date().toISOString().substring(0, 19),
        };

        try {
            if (isEditMode) {
                // 修改事件，发送PUT请求
                const response = await fetch('http://localhost:8080/incident', {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(incidentData),
                });
                if (response.ok) {
                    setIsEditMode(false);
                    setName('');
                    setDescription('');
                    refreshIncidents();
                } else {
                    console.error('修改事件失败，服务器返回状态码非200');
                }
            } else {
                // 创建事件，发送POST请求
                const response = await fetch('http://localhost:8080/incident', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(incidentData),
                });
                if (response.ok) {
                    setName('');
                    setDescription('');
                    refreshIncidents();
                } else {
                    console.error('创建事件失败，服务器返回状态码非200');
                }
            }
        } catch (error) {
            console.error('Error submitting form:', error);
        }
    };

    const enterEditMode = (incident) => {
        setIsEditMode(true);
        setName(incident.name);
        setDescription(incident.description);
        setIncidentIdToEdit(incident.id);
    };

    return (
        <Form onSubmit={handleSubmit}>
            <Form.Group controlId="formName">
                <Form.Label>事件名称</Form.Label>
                <Form.Control
                    type="text"
                    placeholder="输入事件名称"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    isInvalid={!!nameError}
                />
                <Form.Control.Feedback type="invalid">{nameError}</Form.Control.Feedback>
            </Form.Group>

            <Form.Group controlId="formDescription">
                <Form.Label>事件描述</Form.Label>
                <Form.Control
                    type="text"
                    placeholder="输入事件描述"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    isInvalid={!!descriptionError}
                />
                <Form.Control.Feedback type="invalid">{descriptionError}</Form.Control.Feedback>
            </Form.Group>

            <Button variant="primary" type="submit">
                {isEditMode? '修改事件' : '添加事件'}
            </Button>
        </Form>
    );
}

export default IncidentForm;