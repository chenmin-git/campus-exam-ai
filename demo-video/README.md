# Campus Exam AI Demo Video

This directory contains the Remotion source used to render the full feature demo video for the campus intelligent online examination system.

## Commands

```bash
npm install
npm run dev
npx remotion render CampusExamAiDemo ../docs/campus-exam-ai-full-demo.mp4
```

## Assets

- `public/full-demo-scenes.json`: scene metadata for all feature walkthrough steps.
- `public/full-demo-captions.json`: subtitle timeline used by the video.
- `public/full-demo-screens/`: screenshots captured from the running system.

The rendered video is published at `docs/campus-exam-ai-full-demo.mp4`.
