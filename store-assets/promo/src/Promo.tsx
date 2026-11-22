import React from 'react';
import {AbsoluteFill, Audio, Composition, interpolate, registerRoot, Sequence, staticFile, useCurrentFrame} from 'remotion';
import {easeInOut} from './kit';
import {Anime, Dance, End, Hook, Hug, Logo, Magic, Muscle, OldPhoto, Power, Trust} from './scenes';

const XF = 12; // crossfade frames

const SCENES: [React.FC, number][] = [
  [Hook, 105],
  [Logo, 66],
  [Hug, 90],
  [OldPhoto, 90],
  [Dance, 95],
  [Muscle, 85],
  [Power, 105],
  [Magic, 95],
  [Anime, 85],
  [Trust, 90],
  [End, 110],
];

export const TOTAL = SCENES.reduce((a, [, d]) => a + d, 0) - XF * (SCENES.length - 1);

/** Fades, zooms and blurs a scene in and out so neighbours overlap by XF frames. */
const Wrap: React.FC<{dur: number; first: boolean; last: boolean; children: React.ReactNode}> = ({dur, first, last, children}) => {
  const f = useCurrentFrame();
  const inP = first ? 1 : interpolate(f, [0, XF], [0, 1], {extrapolateRight: 'clamp', easing: easeInOut});
  const outP = last ? 0 : interpolate(f, [dur - XF, dur], [0, 1], {extrapolateLeft: 'clamp', easing: easeInOut});
  return (
    <AbsoluteFill style={{opacity: inP * (1 - outP), transform: `scale(${(1.05 - inP * 0.05) * (1 + outP * 0.04)})`, filter: `blur(${(1 - inP) * 12 + outP * 12}px)`}}>
      {children}
    </AbsoluteFill>
  );
};

// Same licensed track as the Sleuth film (Pixabay, see MUSIC.md). Its drop is at 14.23 s:
// start it so the drop lands 4 frames into the logo sting.
const DROP_S = 14.23;
const LOGO_START = SCENES[0][1] - XF;
const MUSIC_TRIM = Math.round((DROP_S - (LOGO_START + 4) / 30) * 30);

const Sound: React.FC = () => {
  const starts: number[] = [];
  let at = 0;
  SCENES.forEach(([, d]) => {
    starts.push(at);
    at += d - XF;
  });
  return (
    <>
      <Audio
        src={staticFile('music.mp3')}
        trimBefore={MUSIC_TRIM}
        volume={(f) => interpolate(f, [0, 8, TOTAL - 60, TOTAL - 2], [0, 0.9, 0.9, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'})}
      />
      {starts.slice(1).map((s, i) =>
        i === 0 ? null : (
          <Sequence key={s} from={Math.max(0, s - 8)} durationInFrames={24}>
            <Audio src={staticFile('whoosh.wav')} volume={0.35} />
          </Sequence>
        ),
      )}
    </>
  );
};

export const Promo: React.FC = () => {
  let at = 0;
  return (
    <AbsoluteFill style={{background: '#140B16'}}>
      <Sound />
      {SCENES.map(([S, d], i) => {
        const from = at;
        at += d - XF;
        return (
          <Sequence key={i} from={from} durationInFrames={d}>
            <Wrap dur={d} first={i === 0} last={i === SCENES.length - 1}>
              <S />
            </Wrap>
          </Sequence>
        );
      })}
    </AbsoluteFill>
  );
};

const Root: React.FC = () => (
  <>
    {/* Google Play promo video (YouTube, landscape) */}
    <Composition id="Promo" component={Promo} durationInFrames={TOTAL} fps={30} width={1920} height={1080} />
    {/* Reels / TikTok / Shorts */}
    <Composition id="PromoVertical" component={Promo} durationInFrames={TOTAL} fps={30} width={1080} height={1920} />
  </>
);

registerRoot(Root);
