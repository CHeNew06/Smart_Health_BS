-- =============================================================
-- Smart Health Management - 完整数据库设计
-- 基于前端 6 大模块 + 后端已有模块
-- BMI 由身高体重在查询时计算，不单独存储
-- =============================================================

-- ----------------------------
-- 1. 用户账号表（已有）
-- ----------------------------
CREATE TABLE IF NOT EXISTS app_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    account     VARCHAR(64)  NOT NULL UNIQUE  COMMENT '登录账号',
    password_hash VARCHAR(255) NOT NULL        COMMENT '密码哈希 BCrypt',
    email       VARCHAR(128) NOT NULL UNIQUE  COMMENT '邮箱',
    nickname    VARCHAR(64)  DEFAULT NULL      COMMENT '昵称',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用, 0 禁用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账号表';

-- ----------------------------
-- 2. 用户资料表（已有）
-- ----------------------------
CREATE TABLE IF NOT EXISTS user_profile (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL UNIQUE COMMENT '关联 app_user.id',
    avatar      VARCHAR(512) DEFAULT NULL     COMMENT '头像 URL',
    gender      TINYINT      DEFAULT NULL     COMMENT '0 保密, 1 男, 2 女',
    birthday    DATE         DEFAULT NULL     COMMENT '出生日期',
    region      VARCHAR(128) DEFAULT NULL     COMMENT '地区',
    signature   VARCHAR(255) DEFAULT NULL     COMMENT '个性签名',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料表';

-- ----------------------------
-- 3. 健康指标记录表（核心表）
-- 存储所有健康指标（血压、心率、体温、血糖、睡眠、呼吸、体重、身高）
-- BMI 在查询时由身高/体重计算，不存储
-- ----------------------------
CREATE TABLE IF NOT EXISTS health_metric (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL          COMMENT '关联 app_user.id',
    metric_type VARCHAR(32)  NOT NULL          COMMENT '指标类型: bp/heartRate/temperature/bloodSugar/sleep/breath/weight/height',
    value1      DECIMAL(10,2) NOT NULL         COMMENT '主值（血压时为收缩压，其他为单值）',
    value2      DECIMAL(10,2) DEFAULT NULL     COMMENT '副值（血压时为舒张压）',
    unit        VARCHAR(16)  NOT NULL          COMMENT '单位: mmHg/BPM/°C/mmol·L/h/次·min/kg/cm',
    record_date DATE         NOT NULL          COMMENT '测量日期',
    record_time TIME         DEFAULT NULL      COMMENT '测量时间',
    notes       VARCHAR(500) DEFAULT NULL      COMMENT '备注',
    source      VARCHAR(16)  DEFAULT 'manual'  COMMENT '来源: manual 手动, voice 语音, device 设备',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_type_date (user_id, metric_type, record_date),
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康指标记录表';

-- ----------------------------
-- 4. 健康评分表（每日一条，定时或请求时计算）
-- ----------------------------
CREATE TABLE IF NOT EXISTS health_score (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    score       INT          NOT NULL          COMMENT '健康评分 0-100',
    body_status VARCHAR(32)  DEFAULT NULL      COMMENT '身体状态描述: 良好/一般/注意',
    score_date  DATE         NOT NULL          COMMENT '评分日期',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, score_date),
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康评分表';

-- ----------------------------
-- 5. 健康建议表
-- ----------------------------
CREATE TABLE IF NOT EXISTS health_advice (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    category    VARCHAR(32)  NOT NULL          COMMENT '分类: diet 饮食, exercise 运动, lifestyle 生活, medical 就医',
    title       VARCHAR(128) NOT NULL          COMMENT '标题',
    content     TEXT         DEFAULT NULL      COMMENT '详细内容',
    tags        VARCHAR(255) DEFAULT NULL      COMMENT '标签（JSON 数组或逗号分隔）',
    advice_date DATE         NOT NULL          COMMENT '建议日期',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, advice_date),
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康建议表';

-- ----------------------------
-- 6. 用药人档案表（一个用户一条）
-- ----------------------------
CREATE TABLE IF NOT EXISTS medication_profile (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL UNIQUE COMMENT '关联 app_user.id',
    name              VARCHAR(64)  DEFAULT NULL    COMMENT '姓名',
    gender            VARCHAR(8)   DEFAULT NULL    COMMENT '性别',
    age               VARCHAR(8)   DEFAULT NULL    COMMENT '年龄',
    drug_allergy      VARCHAR(500) DEFAULT NULL    COMMENT '药物过敏史',
    other_allergy     VARCHAR(500) DEFAULT NULL    COMMENT '食物/其他过敏史',
    chronic_disease   VARCHAR(500) DEFAULT NULL    COMMENT '基础慢性病',
    major_history     VARCHAR(500) DEFAULT NULL    COMMENT '既往重大病史/手术史',
    long_term_meds    VARCHAR(500) DEFAULT NULL    COMMENT '长期固定服用药品',
    emergency_contact VARCHAR(128) DEFAULT NULL    COMMENT '紧急联系人（兼容旧数据）',
    emergency_contact_name  VARCHAR(64)  DEFAULT NULL COMMENT '紧急联系人姓名',
    emergency_contact_phone VARCHAR(20)  DEFAULT NULL COMMENT '紧急联系人手机号',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用药人档案表';

-- ----------------------------
-- 7. 用药记录表（短期 + 长期统一存储，type 区分）
-- ----------------------------
CREATE TABLE IF NOT EXISTS medication_record (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    med_type          VARCHAR(16)  NOT NULL          COMMENT '类型: short_term 短期, long_term 长期/慢性病',
    name              VARCHAR(128) NOT NULL          COMMENT '药品名称',
    purpose           VARCHAR(255) DEFAULT NULL      COMMENT '治疗病症/用途（长期用药）',
    dosage            VARCHAR(64)  DEFAULT NULL      COMMENT '单次剂量',
    frequency         VARCHAR(64)  DEFAULT NULL      COMMENT '医嘱频次',
    contraindication  VARCHAR(500) DEFAULT NULL      COMMENT '用药禁忌',
    record_date       DATE         DEFAULT NULL      COMMENT '用药日期',
    on_time           VARCHAR(16)  DEFAULT NULL      COMMENT '是否按时服用: 是/否/未记录',
    side_effect       VARCHAR(500) DEFAULT NULL      COMMENT '不良反应',
    doctor            VARCHAR(64)  DEFAULT NULL      COMMENT '开具医师',
    remark            VARCHAR(500) DEFAULT NULL      COMMENT '备注（短期用药）',
    follow_up_note    VARCHAR(500) DEFAULT NULL      COMMENT '复诊/调整备注（长期用药）',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_type (user_id, med_type),
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用药记录表';

-- ----------------------------
-- 8. 每日用药提醒/执行表（今日用药 + 标记已服用）
-- ----------------------------
CREATE TABLE IF NOT EXISTS medication_schedule (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    medication_id    BIGINT       DEFAULT NULL      COMMENT '关联 medication_record.id（可选）',
    med_name         VARCHAR(128) NOT NULL          COMMENT '药品名称',
    dosage           VARCHAR(64)  DEFAULT NULL      COMMENT '剂量',
    schedule_time    TIME         NOT NULL          COMMENT '计划服药时间',
    schedule_date    DATE         NOT NULL          COMMENT '计划日期',
    period           VARCHAR(16)  DEFAULT NULL      COMMENT '时段: morning/noon/evening/night',
    status           VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/done/missed',
    taken_at         DATETIME     DEFAULT NULL      COMMENT '实际服药时间',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, schedule_date),
    FOREIGN KEY (user_id) REFERENCES app_user(id),
    FOREIGN KEY (medication_id) REFERENCES medication_record(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日用药提醒表';

-- ----------------------------
-- 9. 健康计划表（当前生效的计划，一个用户一条活跃计划）
-- ----------------------------
CREATE TABLE IF NOT EXISTS health_plan (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(128) DEFAULT NULL      COMMENT '计划标题',
    status      VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT 'active/completed/archived',
    source      VARCHAR(16)  DEFAULT 'ai'     COMMENT '来源: ai AI生成, manual 手动',
    start_date  DATE         DEFAULT NULL      COMMENT '计划开始日期',
    end_date    DATE         DEFAULT NULL      COMMENT '计划结束日期',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status),
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康计划表';

-- ----------------------------
-- 10. 运动计划表（按周几存储）
-- ----------------------------
CREATE TABLE IF NOT EXISTS plan_exercise (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id     BIGINT       NOT NULL          COMMENT '关联 health_plan.id',
    user_id     BIGINT       NOT NULL,
    day_of_week TINYINT      NOT NULL          COMMENT '星期几: 1=周一 ... 7=周日',
    name        VARCHAR(64)  NOT NULL          COMMENT '运动名称',
    duration    VARCHAR(32)  DEFAULT NULL      COMMENT '时长（如 30分钟）',
    intensity   VARCHAR(16)  DEFAULT NULL      COMMENT '强度: low/medium/high',
    is_aerobic  TINYINT(1)   DEFAULT 1         COMMENT '1 有氧, 0 无氧',
    sort_order  INT          DEFAULT 0         COMMENT '排序',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plan_day (plan_id, day_of_week),
    FOREIGN KEY (plan_id) REFERENCES health_plan(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动计划表';

-- ----------------------------
-- 11. 饮食计划表（按周几 + 餐次存储）
-- ----------------------------
CREATE TABLE IF NOT EXISTS plan_diet (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id     BIGINT       NOT NULL          COMMENT '关联 health_plan.id',
    user_id     BIGINT       NOT NULL,
    day_of_week TINYINT      NOT NULL          COMMENT '星期几: 1~7',
    meal_type   VARCHAR(16)  NOT NULL          COMMENT '餐次: breakfast/lunch/dinner/snack',
    calories    INT          DEFAULT NULL      COMMENT '热量 kcal',
    foods       VARCHAR(500) DEFAULT NULL      COMMENT '食物列表（JSON 数组或逗号分隔）',
    sort_order  INT          DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plan_day (plan_id, day_of_week),
    FOREIGN KEY (plan_id) REFERENCES health_plan(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮食计划表';

-- ----------------------------
-- 12. 饮食计划营养目标（每日目标，全局或按天）
-- ----------------------------
CREATE TABLE IF NOT EXISTS plan_diet_target (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id        BIGINT       NOT NULL          COMMENT '关联 health_plan.id',
    user_id        BIGINT       NOT NULL,
    daily_calories INT          DEFAULT NULL      COMMENT '每日目标热量 kcal',
    carb_target    INT          DEFAULT NULL      COMMENT '碳水目标 g',
    protein_target INT          DEFAULT NULL      COMMENT '蛋白质目标 g',
    fat_target     INT          DEFAULT NULL      COMMENT '脂肪目标 g',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_plan_user (plan_id, user_id),
    FOREIGN KEY (plan_id) REFERENCES health_plan(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮食计划营养目标表';

-- ----------------------------
-- 13. 复查计划表
-- ----------------------------
CREATE TABLE IF NOT EXISTS plan_checkup (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    plan_id     BIGINT       DEFAULT NULL      COMMENT '关联 health_plan.id（可选）',
    checkup_date DATE        NOT NULL          COMMENT '复查日期',
    hospital    VARCHAR(128) DEFAULT NULL      COMMENT '医院/诊所',
    items       VARCHAR(500) DEFAULT NULL      COMMENT '检查项目（JSON 数组或逗号分隔）',
    note        VARCHAR(500) DEFAULT NULL      COMMENT '备注',
    status      VARCHAR(16)  NOT NULL DEFAULT 'upcoming' COMMENT 'upcoming/done/expired',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, checkup_date),
    FOREIGN KEY (user_id) REFERENCES app_user(id),
    FOREIGN KEY (plan_id) REFERENCES health_plan(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复查计划表';

-- ----------------------------
-- 14. 每日任务表（统一管理各类任务）
-- ----------------------------
CREATE TABLE IF NOT EXISTS daily_task (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    plan_id     BIGINT       DEFAULT NULL      COMMENT '关联 health_plan.id',
    title       VARCHAR(128) NOT NULL          COMMENT '任务标题',
    task_type   VARCHAR(32)  NOT NULL          COMMENT '类型: exercise 运动, diet 饮食, medication 用药, health 健康',
    task_time   VARCHAR(32)  DEFAULT NULL      COMMENT '任务时间描述（如 08:00）',
    task_date   DATE         NOT NULL          COMMENT '任务日期',
    status      VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/done/missed',
    completed_at DATETIME    DEFAULT NULL      COMMENT '完成时间',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, task_date),
    INDEX idx_user_date_status (user_id, task_date, status),
    FOREIGN KEY (user_id) REFERENCES app_user(id),
    FOREIGN KEY (plan_id) REFERENCES health_plan(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日任务表';

-- ----------------------------
-- 15. AI 咨询对话表
-- ----------------------------
CREATE TABLE IF NOT EXISTS consult_conversation (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(128) DEFAULT NULL      COMMENT '对话标题',
    mode        VARCHAR(16)  NOT NULL DEFAULT 'chat' COMMENT '模式: chat 智能问答, inquiry AI问诊',
    summary     VARCHAR(500) DEFAULT NULL      COMMENT '对话摘要',
    msg_count   INT          NOT NULL DEFAULT 0 COMMENT '消息条数',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, created_at DESC),
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 咨询对话表';

-- ----------------------------
-- 16. AI 咨询消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS consult_message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT       NOT NULL          COMMENT '关联 consult_conversation.id',
    user_id         BIGINT       NOT NULL,
    role            VARCHAR(16)  NOT NULL          COMMENT '角色: user/ai/system',
    content         TEXT         NOT NULL          COMMENT '消息内容',
    image_url       VARCHAR(512) DEFAULT NULL      COMMENT '图片 URL（可选）',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conv_time (conversation_id, created_at),
    FOREIGN KEY (conversation_id) REFERENCES consult_conversation(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 咨询消息表';

-- ----------------------------
-- 17. AI 问诊问题表（AI主动问诊的问题列表）
-- ----------------------------
CREATE TABLE IF NOT EXISTS consult_inquiry_question (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT       NOT NULL          COMMENT '关联 consult_conversation.id',
    question        VARCHAR(500) NOT NULL          COMMENT '问题内容',
    answer          VARCHAR(500) DEFAULT NULL      COMMENT '用户回答',
    sort_order      INT          DEFAULT 0         COMMENT '排序',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES consult_conversation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 问诊问题表';

-- ----------------------------
-- 18. 用户打卡/统计表（连续打卡、完成率等）
-- ----------------------------
CREATE TABLE IF NOT EXISTS user_checkin (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    checkin_date DATE        NOT NULL          COMMENT '打卡日期',
    total_tasks INT          DEFAULT 0         COMMENT '当日总任务数',
    done_tasks  INT          DEFAULT 0         COMMENT '当日完成任务数',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, checkin_date),
    FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日打卡统计表';

-- =============================================================
-- 演示数据
-- =============================================================
INSERT INTO app_user (account, password_hash, email, nickname, status)
SELECT 'elder001', '123456', 'elder001@example.com', 'Demo Elder', 1
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE account = 'elder001');
