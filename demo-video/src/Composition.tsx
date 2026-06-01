import type { Caption } from "@remotion/captions";
import {
  AbsoluteFill,
  Easing,
  Img,
  interpolate,
  Sequence,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import captionsData from "../public/full-demo-captions.json";
import scenesData from "../public/full-demo-scenes.json";

export const VIDEO_WIDTH = 1280;
export const VIDEO_HEIGHT = 720;
export const VIDEO_FPS = 30;

const COVER_FRAMES = 90;
const MODULE_FRAMES = 120;
const ARCH_FRAMES = 120;
const INTRO_FRAMES = COVER_FRAMES + MODULE_FRAMES + ARCH_FRAMES;
const SCENE_FRAMES = 90;
const OUTRO_FRAMES = 120;

type DemoScene = {
  image: string;
  title: string;
  role: string;
  tags: string[];
  caption: string;
};

const scenes = scenesData as DemoScene[];
const captions = captionsData as Caption[];

export const TOTAL_FRAMES = INTRO_FRAMES + scenes.length * SCENE_FRAMES + OUTRO_FRAMES;

const palette = {
  navy: "#0f172a",
  ink: "#172033",
  panel: "rgba(255,255,255,0.92)",
  accent: "#00a1d6",
  pink: "#fb7299",
  green: "#48b685",
  line: "rgba(148,163,184,0.34)",
};

export const DemoVideo: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill style={styles.root}>
      <Background />
      <Sequence durationInFrames={INTRO_FRAMES}>
        <Sequence durationInFrames={COVER_FRAMES}>
          <CoverIntro />
        </Sequence>
        <Sequence from={COVER_FRAMES} durationInFrames={MODULE_FRAMES}>
          <ModuleIntro />
        </Sequence>
        <Sequence from={COVER_FRAMES + MODULE_FRAMES} durationInFrames={ARCH_FRAMES}>
          <ArchitectureIntro />
        </Sequence>
      </Sequence>
      {scenes.map((scene, index) => (
        <Sequence
          key={`${scene.image}-${index}`}
          from={INTRO_FRAMES + index * SCENE_FRAMES}
          durationInFrames={SCENE_FRAMES}
        >
          <SceneCard scene={scene} index={index} />
        </Sequence>
      ))}
      <Sequence from={INTRO_FRAMES + scenes.length * SCENE_FRAMES} durationInFrames={OUTRO_FRAMES}>
        <Outro />
      </Sequence>
      <CaptionBar captions={captions} currentMs={(frame / fps) * 1000} />
    </AbsoluteFill>
  );
};

const Background: React.FC = () => (
  <AbsoluteFill
    style={{
      background:
        "radial-gradient(circle at 14% 15%, rgba(0,161,214,0.25), transparent 28%), radial-gradient(circle at 84% 20%, rgba(251,114,153,0.22), transparent 30%), linear-gradient(135deg, #eef7fb 0%, #f7fafc 47%, #f0fdf4 100%)",
    }}
  >
    <div style={styles.gridOverlay} />
  </AbsoluteFill>
);

const CoverIntro: React.FC = () => {
  const frame = useCurrentFrame();
  const opacity = fade(frame, 0, 20, 70, COVER_FRAMES);
  const y = interpolate(frame, [0, 34], [28, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });

  return (
    <AbsoluteFill style={{ ...styles.center, opacity, transform: `translateY(${y}px)` }}>
      <div style={styles.kicker}>Campus Exam AI</div>
      <div style={styles.heroTitle}>校园智能在线考试系统</div>
      <div style={styles.heroSubtitle}>
        {scenes.length} 个真实功能动作演示，覆盖新增、修改、导入、考试、阅卷、分析、监控、申诉和通知
      </div>
      <div style={styles.heroTags}>
        {["管理员", "教师", "学生", "新增修改", "AI 能力", "完整闭环"].map((tag) => (
          <span key={tag} style={styles.heroTag}>
            {tag}
          </span>
        ))}
      </div>
    </AbsoluteFill>
  );
};

const ModuleIntro: React.FC = () => {
  const frame = useCurrentFrame();
  const opacity = fade(frame, 0, 18, MODULE_FRAMES - 18, MODULE_FRAMES);
  const modules = [
    {
      role: "管理员端",
      color: palette.accent,
      items: ["用户/班级/课程", "授课安排/公告", "权限/日志/备份"],
    },
    {
      role: "教师端",
      color: palette.pink,
      items: ["题库导入/查重/审核", "手动组卷/AI 组卷", "阅卷/分析/监控"],
    },
    {
      role: "学生端",
      color: palette.green,
      items: ["在线考试/历史成绩", "错题解析/学习建议", "申诉/通知/个人中心"],
    },
  ];

  return (
    <AbsoluteFill style={{ ...styles.diagramSlide, opacity }}>
      <div style={styles.kicker}>功能模块图</div>
      <div style={styles.diagramTitle}>三端角色覆盖完整考试业务闭环</div>
      <div style={styles.moduleGrid}>
        {modules.map((module, index) => {
          const y = interpolate(frame, [index * 8, index * 8 + 20], [24, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          });
          return (
            <div
              key={module.role}
              style={{
                ...styles.moduleCard,
                borderTop: `7px solid ${module.color}`,
                transform: `translateY(${y}px)`,
              }}
            >
              <strong style={styles.moduleRole}>{module.role}</strong>
              {module.items.map((item) => (
                <span key={item} style={styles.moduleItem}>
                  {item}
                </span>
              ))}
            </div>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};

const ArchitectureIntro: React.FC = () => {
  const frame = useCurrentFrame();
  const opacity = fade(frame, 0, 18, ARCH_FRAMES - 18, ARCH_FRAMES);
  const nodes = [
    ["浏览器用户", "管理员 / 教师 / 学生"],
    ["Vue 3 前端", "Element Plus + Pinia + Router"],
    ["Spring Boot API", "鉴权 / 考试 / 阅卷 / 统计"],
    ["MySQL 数据库", "题库 / 试卷 / 答案 / 日志"],
    ["讯飞星火 API", "出题 / 组卷 / 错题建议"],
  ];

  return (
    <AbsoluteFill style={{ ...styles.diagramSlide, opacity }}>
      <div style={styles.kicker}>系统框架图</div>
      <div style={styles.diagramTitle}>前后端分层、数据闭环、AI 能力可插拔</div>
      <div style={styles.archGrid}>
        {nodes.map(([title, desc], index) => {
          const x = interpolate(frame, [index * 7, index * 7 + 18], [22, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          });
          return (
            <div key={title} style={styles.archStepWrap}>
              <div style={{ ...styles.archCard, transform: `translateX(${x}px)` }}>
                <strong style={styles.archCardTitle}>{title}</strong>
                <span style={styles.archCardDesc}>{desc}</span>
              </div>
              {index < nodes.length - 1 ? <div style={styles.archArrow}>→</div> : null}
            </div>
          );
        })}
      </div>
      <div style={styles.archFooter}>
        监控事件、通知提醒、成绩申诉、操作日志贯穿考试全过程
      </div>
    </AbsoluteFill>
  );
};

const SceneCard: React.FC<{ scene: DemoScene; index: number }> = ({ scene, index }) => {
  const frame = useCurrentFrame();
  const opacity = fade(frame, 0, 6, SCENE_FRAMES - 8, SCENE_FRAMES);
  const titleX = interpolate(frame, [0, 18], [-16, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });

  return (
    <AbsoluteFill style={{ opacity }}>
      <div style={styles.header}>
        <div>
          <div style={styles.rolePill}>{scene.role}</div>
          <div style={{ ...styles.sceneTitle, transform: `translateX(${titleX}px)` }}>{scene.title}</div>
        </div>
        <div style={styles.counter}>
          {String(index + 1).padStart(2, "0")} / {scenes.length}
        </div>
      </div>
      <div style={styles.screenshotFrame}>
        <Img
          src={staticFile(`full-demo-screens/${scene.image}`)}
          style={styles.screenshot}
        />
      </div>
      <div style={styles.tagRow}>
        {scene.tags.map((tag) => (
          <span key={tag} style={styles.tag}>
            {tag}
          </span>
        ))}
      </div>
      <div style={styles.progressTrack}>
        <div
          style={{
            ...styles.progressFill,
            width: `${((index + frame / SCENE_FRAMES) / scenes.length) * 100}%`,
          }}
        />
      </div>
    </AbsoluteFill>
  );
};

const Outro: React.FC = () => {
  const frame = useCurrentFrame();
  const opacity = fade(frame, 0, 22, 92, OUTRO_FRAMES);
  const scale = interpolate(frame, [0, 42], [0.96, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });

  return (
    <AbsoluteFill style={{ ...styles.center, opacity, transform: `scale(${scale})` }}>
      <div style={styles.kicker}>Open Source Ready</div>
      <div style={styles.heroTitle}>完整功能演示完成</div>
      <div style={styles.heroSubtitle}>视频素材由自动化测试重新采集，字幕与画面同步，可继续用 Remotion 修改和重渲染</div>
      <div style={styles.summaryBox}>
        <div>43 个功能场景</div>
        <div>{captions.length} 条字幕说明</div>
        <div>自动化截图与测试报告</div>
      </div>
    </AbsoluteFill>
  );
};

const CaptionBar: React.FC<{ captions: Caption[]; currentMs: number }> = ({ captions, currentMs }) => {
  const active = captions.find((caption) => currentMs >= caption.startMs && currentMs < caption.endMs);

  return (
    <AbsoluteFill style={{ justifyContent: "flex-end", pointerEvents: "none" }}>
      <div style={styles.captionWrap}>
        <div style={styles.caption}>{active?.text ?? ""}</div>
      </div>
    </AbsoluteFill>
  );
};

const fade = (frame: number, inStart: number, inEnd: number, outStart: number, outEnd: number) => {
  const fadeIn = interpolate(frame, [inStart, inEnd], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const fadeOut = interpolate(frame, [outStart, outEnd], [1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  return Math.min(fadeIn, fadeOut);
};

const styles: Record<string, React.CSSProperties> = {
  root: {
    color: palette.ink,
    overflow: "hidden",
  },
  gridOverlay: {
    position: "absolute",
    inset: 0,
    backgroundImage:
      "linear-gradient(rgba(15,23,42,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(15,23,42,0.04) 1px, transparent 1px)",
    backgroundSize: "42px 42px",
    maskImage: "linear-gradient(to bottom, rgba(0,0,0,0.55), transparent 78%)",
  },
  center: {
    alignItems: "center",
    justifyContent: "center",
    textAlign: "center",
    padding: "88px 120px 150px",
  },
  kicker: {
    display: "inline-flex",
    padding: "8px 16px",
    borderRadius: 999,
    background: "rgba(0,161,214,0.14)",
    color: "#03698b",
    fontSize: 24,
    fontWeight: 800,
    letterSpacing: 0,
  },
  heroTitle: {
    marginTop: 22,
    fontSize: 62,
    lineHeight: 1.08,
    fontWeight: 900,
    color: palette.navy,
  },
  heroSubtitle: {
    marginTop: 20,
    maxWidth: 920,
    fontSize: 27,
    lineHeight: 1.45,
    color: "#475569",
  },
  heroTags: {
    display: "flex",
    gap: 14,
    flexWrap: "wrap",
    justifyContent: "center",
    marginTop: 34,
  },
  heroTag: {
    padding: "10px 18px",
    borderRadius: 10,
    background: palette.panel,
    border: `1px solid ${palette.line}`,
    fontSize: 22,
    fontWeight: 800,
    color: "#155a78",
  },
  diagramSlide: {
    padding: "62px 72px 130px",
    justifyContent: "center",
  },
  diagramTitle: {
    marginTop: 16,
    fontSize: 42,
    lineHeight: 1.18,
    fontWeight: 900,
    color: palette.navy,
  },
  moduleGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(3, 1fr)",
    gap: 20,
    marginTop: 36,
  },
  moduleCard: {
    minHeight: 270,
    padding: "26px 24px",
    borderRadius: 16,
    background: "rgba(255,255,255,0.92)",
    border: `1px solid ${palette.line}`,
    boxShadow: "0 18px 46px rgba(15,23,42,0.13)",
  },
  moduleRole: {
    display: "block",
    color: palette.navy,
    fontSize: 31,
    lineHeight: 1.15,
    marginBottom: 18,
  },
  moduleItem: {
    display: "block",
    marginTop: 11,
    padding: "10px 12px",
    borderRadius: 10,
    background: "#f8fbff",
    border: `1px solid ${palette.line}`,
    color: "#334155",
    fontSize: 22,
    fontWeight: 800,
  },
  archGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(5, minmax(0, 1fr))",
    gap: 12,
    alignItems: "stretch",
    marginTop: 38,
  },
  archStepWrap: {
    display: "grid",
    gridTemplateColumns: "minmax(0, 1fr) 24px",
    gap: 8,
    alignItems: "center",
  },
  archCard: {
    minHeight: 178,
    padding: "22px 18px",
    borderRadius: 16,
    background: "rgba(255,255,255,0.92)",
    border: `1px solid ${palette.line}`,
    boxShadow: "0 18px 46px rgba(15,23,42,0.13)",
  },
  archCardTitle: {
    display: "block",
    color: palette.navy,
    fontSize: 26,
    lineHeight: 1.18,
    fontWeight: 900,
  },
  archCardDesc: {
    display: "block",
    marginTop: 14,
    color: "#475569",
    fontSize: 20,
    lineHeight: 1.36,
    fontWeight: 750,
  },
  archArrow: {
    color: palette.accent,
    fontSize: 28,
    fontWeight: 900,
  },
  archFooter: {
    marginTop: 30,
    padding: "14px 18px",
    borderRadius: 14,
    background: "rgba(15,23,42,0.82)",
    color: "#fff",
    fontSize: 25,
    fontWeight: 850,
    textAlign: "center",
  },
  header: {
    position: "absolute",
    left: 56,
    right: 56,
    top: 28,
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
  },
  rolePill: {
    display: "inline-flex",
    padding: "6px 13px",
    borderRadius: 999,
    color: "#065f46",
    background: "rgba(72,182,133,0.16)",
    fontWeight: 900,
    fontSize: 17,
  },
  sceneTitle: {
    marginTop: 8,
    fontSize: 39,
    lineHeight: 1.12,
    fontWeight: 900,
    color: palette.navy,
  },
  counter: {
    padding: "10px 14px",
    borderRadius: 10,
    background: "rgba(15,23,42,0.82)",
    color: "#fff",
    fontSize: 20,
    fontWeight: 900,
  },
  screenshotFrame: {
    position: "absolute",
    left: 56,
    right: 56,
    top: 116,
    height: 466,
    borderRadius: 16,
    overflow: "hidden",
    background: "#fff",
    border: `1px solid ${palette.line}`,
    boxShadow: "0 24px 70px rgba(15,23,42,0.20)",
  },
  screenshot: {
    width: "100%",
    height: "100%",
    objectFit: "cover",
    objectPosition: "top center",
  },
  tagRow: {
    position: "absolute",
    left: 56,
    right: 56,
    top: 597,
    display: "flex",
    gap: 10,
    flexWrap: "wrap",
  },
  tag: {
    padding: "7px 13px",
    borderRadius: 8,
    background: "rgba(255,255,255,0.9)",
    border: `1px solid ${palette.line}`,
    color: "#155a78",
    fontSize: 17,
    fontWeight: 850,
  },
  progressTrack: {
    position: "absolute",
    left: 56,
    right: 56,
    bottom: 84,
    height: 5,
    borderRadius: 999,
    background: "rgba(15,23,42,0.12)",
    overflow: "hidden",
  },
  progressFill: {
    height: "100%",
    borderRadius: 999,
    background: `linear-gradient(90deg, ${palette.accent}, ${palette.pink}, ${palette.green})`,
  },
  captionWrap: {
    margin: "0 56px 22px",
    minHeight: 54,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "11px 22px",
    borderRadius: 12,
    background: "rgba(15,23,42,0.84)",
    boxShadow: "0 16px 36px rgba(15,23,42,0.24)",
  },
  caption: {
    color: "#fff",
    fontSize: 23,
    lineHeight: 1.35,
    fontWeight: 750,
    textAlign: "center",
  },
  summaryBox: {
    marginTop: 34,
    display: "flex",
    gap: 12,
    flexWrap: "wrap",
    justifyContent: "center",
    padding: "18px 24px",
    borderRadius: 14,
    background: "rgba(255,255,255,0.92)",
    border: `1px solid ${palette.line}`,
    color: "#155a78",
    fontSize: 22,
    fontWeight: 800,
  },
};
