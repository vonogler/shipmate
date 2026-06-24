import { Button, Col, Form, Input, Row, Select, Table, Typography } from 'antd';
import type { FormInstance } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { UserAddOutlined } from '@ant-design/icons';
import type { AdminUser, LoginFormValues } from '../types';

type UsersScreenProps = {
  form: FormInstance<LoginFormValues>;
  users: AdminUser[];
  columns: ColumnsType<AdminUser>;
  loading: boolean;
  creatingUser: boolean;
  onCreateUser: (values: LoginFormValues) => void;
};

export default function UsersScreen({
  form,
  users,
  columns,
  loading,
  creatingUser,
  onCreateUser
}: UsersScreenProps) {
  return (
    <section className="work-panel">
      <div className="section-heading">
        <Typography.Title level={2}>내부 직원 계정</Typography.Title>
        <Typography.Text type="secondary">운영자와 사원 계정을 생성하고 권한을 확인합니다.</Typography.Text>
      </div>
      <Form form={form} layout="vertical" initialValues={{ role: 'OPERATOR' }} onFinish={onCreateUser}>
        <Row gutter={12}>
          <Col xs={24} md={6}>
            <Form.Item name="loginId" label="로그인 ID" rules={[{ required: true, message: '로그인 ID를 입력하세요.' }]}>
              <Input />
            </Form.Item>
          </Col>
          <Col xs={24} md={6}>
            <Form.Item name="password" label="임시 비밀번호" rules={[{ required: true, message: '임시 비밀번호를 입력하세요.' }]}>
              <Input.Password />
            </Form.Item>
          </Col>
          <Col xs={24} md={6}>
            <Form.Item name="displayName" label="이름" rules={[{ required: true, message: '이름을 입력하세요.' }]}>
              <Input />
            </Form.Item>
          </Col>
          <Col xs={24} md={6}>
            <Form.Item name="role" label="권한">
              <Select options={[
                { value: 'ADMIN', label: '관리자' },
                { value: 'OPERATOR', label: '운영자' },
                { value: 'VIEWER', label: '조회자' }
              ]} />
            </Form.Item>
          </Col>
        </Row>
        <Button type="primary" htmlType="submit" icon={<UserAddOutlined />} loading={creatingUser}>
          직원 계정 생성
        </Button>
      </Form>
      <div className="table-gap">
        <Table
          rowKey="merchantUserId"
          columns={columns}
          dataSource={users}
          loading={loading}
          scroll={{ x: 1080 }}
          pagination={{ pageSize: 12, showSizeChanger: false }}
        />
      </div>
    </section>
  );
}
