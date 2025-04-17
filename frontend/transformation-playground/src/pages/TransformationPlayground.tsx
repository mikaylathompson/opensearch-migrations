import Container from '@cloudscape-design/components/container';
import Grid from '@cloudscape-design/components/grid';
import InputDocumentSection from './InputDocumentSection';
import TransformationSection from './TransformationSection';
import OutputDocumentSection from './OutputDocumentSection';
import { PlaygroundProvider } from '../context/PlaygroundContext';

export default function TransformationPlayground() {
  return (
    <Container>
      <PlaygroundProvider>
        <Grid
          gridDefinition={[
            { colspan: { default: 9, m: 3 } },
            { colspan: { default: 9, m: 6 } },
            { colspan: { default: 9, m: 3 } }
          ]}
        >
          <InputDocumentSection />
          <TransformationSection />
          <OutputDocumentSection />
        </Grid>
      </PlaygroundProvider>
    </Container>
  );
}
