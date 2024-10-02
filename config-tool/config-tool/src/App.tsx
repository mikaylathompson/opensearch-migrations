import { useState } from "react";
import "@cloudscape-design/global-styles/index.css";

import Button from "@cloudscape-design/components/button";
import Header from "@cloudscape-design/components/header";
import Container from "@cloudscape-design/components/container";
import SpaceBetween from "@cloudscape-design/components/space-between";
import Input from "@cloudscape-design/components/input";
import Form from "@cloudscape-design/components/form";
import FormField from "@cloudscape-design/components/form-field";
import {
  Checkbox,
  ContentLayout,
  RadioGroup,
  Select,
} from "@cloudscape-design/components";
import { CodeView } from "@cloudscape-design/code-view";
import CopyToClipboard from "@cloudscape-design/components/copy-to-clipboard";
import ConfigForm from "./components/ConfigForm";

type FormContext = {
  vpcId: string;
  useRfs: boolean;
  useReplayer: boolean;
  sourceClusterEndpoint: string;
  sourceClusterAuthType: string;
  sourceClusterAuthUsername: string | undefined;
  sourceClusterAuthPasswordFromArn: string | undefined;
  sourceClusterAuthServiceSigningName: string | undefined;
  sourceClusterAuthRegion: string | undefined;
  targetClusterEndpoint: string | undefined;
  targetClusterAuthType: string;
  targetClusterAuthUsername: string | undefined;
  targetClusterAuthPasswordFromArn: string | undefined;
  targetClusterAuthServiceSigningName: string | undefined;
  targetClusterAuthRegion: string | undefined;
};

const emptyFormContext = {
  useReplayer: false,
  useRfs: false,
  vpcId: "",
  sourceClusterEndpoint: "",
  sourceClusterAuthType: "none",
  sourceClusterAuthUsername: undefined,
  sourceClusterAuthPasswordFromArn: undefined,
  sourceClusterAuthServiceSigningName: undefined,
  sourceClusterAuthRegion: undefined,
  targetClusterEndpoint: "",
  targetClusterAuthType: "none",
  targetClusterAuthUsername: undefined,
  targetClusterAuthPasswordFromArn: undefined,
  targetClusterAuthServiceSigningName: undefined,
  targetClusterAuthRegion: undefined,
};

const regionList = [
  "us-east-1",
  "us-east-2",
  "us-west-1",
  "us-west-2",
  "eu-west-1",
  "eu-west-2",
  "ap-northeast-1",
  "ap-northeast-2",
];

function App() {
  const [contextJson, setContextJson] = useState("");
  const [formValues, setFormValues] = useState<FormContext>(emptyFormContext);

  const partialStateUpdate = (update: Partial<FormContext>) =>
    setFormValues({ ...formValues, ...update } as FormContext);

  const formatContextJson = () => {
    const contextObj = {
      default: {
        stage: "dev",
        reindexFromSnapshotServiceEnabled: formValues.useRfs,
        trafficReplayerServiceEnabled: formValues.useReplayer,
        vpcId: formValues.vpcId,
        sourceCluster: {
          endpoint: formValues.sourceClusterEndpoint,
          auth: formValues.sourceClusterAuthType,
          username: formValues.sourceClusterAuthUsername,
          password_from_secret_arn: formValues.sourceClusterAuthPasswordFromArn,
          serviceSigningName: formValues.sourceClusterAuthServiceSigningName,
          region: formValues.sourceClusterAuthRegion,
        },
        targetCluster: {
          endpoint: formValues.targetClusterEndpoint,
          auth: formValues.targetClusterAuthType,
          username: formValues.targetClusterAuthUsername,
          password_from_secret_arn: formValues.targetClusterAuthPasswordFromArn,
          serviceSigningName: formValues.targetClusterAuthServiceSigningName,
          region: formValues.targetClusterAuthRegion,
        },
      },
    };
    setContextJson(JSON.stringify(contextObj, null, 2));
  };

  return (
    <ContentLayout
      defaultPadding
      header={<Header variant="h1">Migration Assistant Config Tool</Header>}
    >
      <Container>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            formatContextJson();
          }}
        >
          <Form
            actions={
              <SpaceBetween direction="horizontal" size="xs">
                <Button onClick={() => setContextJson("")}>Cancel</Button>
                <Button variant="primary">Submit</Button>
              </SpaceBetween>
            }
          >
            <Container>
              <ConfigForm
                formValues={formValues}
                partialStateUpdate={partialStateUpdate}
                regionList={regionList}
              />
            </Container>
          </Form>
        </form>
        {contextJson ? (
          <CodeView
            content={contextJson}
            lineNumbers
            actions={
              <CopyToClipboard
                copyButtonAriaLabel="Copy code"
                copyErrorText="Code failed to copy"
                copySuccessText="Code copied"
                textToCopy='const hello: string = "world";
  console.log(hello);'
              />
            }
          />
        ) : null}
      </Container>
    </ContentLayout>
  );
}

export default App;
